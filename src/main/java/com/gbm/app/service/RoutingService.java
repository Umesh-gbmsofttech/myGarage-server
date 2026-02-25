package com.gbm.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbm.app.dto.DirectionsRequest;
import com.gbm.app.dto.DirectionsResponse;
import com.gbm.app.dto.RoutingStatsResponse;
import com.gbm.app.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RoutingService {

    private static final Logger logger = LoggerFactory.getLogger(RoutingService.class);

    private static final String PUBLIC_OSRM_URL = "https://router.project-osrm.org/route/v1/driving/";
    private static final String ORS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
    private static final long CACHE_TTL_MS = 14 * 60 * 1000L;

    @org.springframework.beans.factory.annotation.Value("${api.keys.ors:}")
    private String orsKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CacheEntry> routeCache = new ConcurrentHashMap<>();
    private final AtomicLong inboundRequests = new AtomicLong(0);
    private final AtomicLong outboundRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);

    public RoutingService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(6))
                .setReadTimeout(Duration.ofSeconds(12))
                .build();
        this.objectMapper = objectMapper;
    }

    public DirectionsResponse getDirections(DirectionsRequest request) {
        validateRequest(request);
        inboundRequests.incrementAndGet();

        String provider = (orsKey != null && !orsKey.isBlank()) ? "ors" : "osrm";

        logger.info(
                "Routing request received provider={} fromLat={} fromLng={} toLat={} toLng={}",
                provider,
                request.getFromLat(),
                request.getFromLng(),
                request.getToLat(),
                request.getToLng()
        );

        String cacheKey = cacheKey(request);
        DirectionsResponse cached = getCachedRoute(cacheKey);
        if (cached != null) {
            cacheHits.incrementAndGet();
            logger.info("Routing cache hit provider={} key={}", provider, cacheKey);
            return cached;
        }

        DirectionsResponse fresh;
        if ("ors".equals(provider)) {
            fresh = fetchRouteFromORS(request);
        } else {
            fresh = fetchRouteFromPublicOsrm(request);
        }
        
        routeCache.put(cacheKey, new CacheEntry(fresh, System.currentTimeMillis() + CACHE_TTL_MS));
        return fresh;
    }

    private DirectionsResponse fetchRouteFromORS(DirectionsRequest request) {
        String url = String.format(Locale.US, "%s?api_key=%s&start=%.6f,%.6f&end=%.6f,%.6f", 
                ORS_URL, orsKey, request.getFromLng(), request.getFromLat(), request.getToLng(), request.getToLat());

        long startedAt = System.currentTimeMillis();
        try {
            outboundRequests.incrementAndGet();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            long elapsedMs = System.currentTimeMillis() - startedAt;
            logger.info("Routing provider response provider=ors status={} elapsedMs={}", response.getStatusCode().value(), elapsedMs);
            return parseOrsDirections(response.getBody());
        } catch (Exception ex) {
            logger.error("ORS routing failed, falling back to OSRM", ex);
            return fetchRouteFromPublicOsrm(request);
        }
    }

    private DirectionsResponse parseOrsDirections(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "No routes found in ORS");
            }

            JsonNode feature = features.path(0);
            JsonNode geometry = feature.path("geometry");
            JsonNode coordinates = geometry.path("coordinates");
            List<DirectionsResponse.RoutePoint> routePoints = parseCoordinates(coordinates);

            JsonNode properties = feature.path("properties");
            JsonNode summary = properties.path("summary");
            double distanceMeters = summary.path("distance").asDouble(0);
            double durationSeconds = summary.path("duration").asDouble(0);

            List<DirectionsResponse.RouteStep> steps = new ArrayList<>();
            JsonNode segments = properties.path("segments");
            if (segments.isArray() && !segments.isEmpty()) {
                JsonNode stepsNode = segments.path(0).path("steps");
                if (stepsNode.isArray()) {
                    for (JsonNode step : stepsNode) {
                        steps.add(new DirectionsResponse.RouteStep(
                                step.path("instruction").asText("Continue"),
                                step.path("distance").asDouble(0),
                                step.path("duration").asDouble(0),
                                step.path("type").asInt()
                        ));
                    }
                }
            }

            return toResponse(routePoints, distanceMeters, durationSeconds, steps);
        } catch (Exception ex) {
            logger.error("Failed to parse ORS response", ex);
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing parse failed");
        }
    }

    private DirectionsResponse fetchRouteFromPublicOsrm(DirectionsRequest request) {
        String start = String.format(Locale.US, "%.6f,%.6f", request.getFromLng(), request.getFromLat());
        String end = String.format(Locale.US, "%.6f,%.6f", request.getToLng(), request.getToLat());
        String url = PUBLIC_OSRM_URL + start + ";" + end + "?overview=full&geometries=geojson&steps=true";

        long startedAt = System.currentTimeMillis();
        try {
            outboundRequests.incrementAndGet();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            long elapsedMs = System.currentTimeMillis() - startedAt;
            logger.info("Routing provider response provider=osrm status={} elapsedMs={}", response.getStatusCode().value(), elapsedMs);
            return parseOsrmDirections(response.getBody());
        } catch (HttpStatusCodeException ex) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            logger.error(
                    "Routing failed provider=osrm elapsedMs={} upstreamStatus={} responseBody={}",
                    elapsedMs,
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider unavailable");
        } catch (ResourceAccessException ex) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            logger.error("Routing timeout/network failure provider=osrm elapsedMs={} message={}", elapsedMs, ex.getMessage(), ex);
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider unavailable");
        } catch (UpstreamServiceException ex) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            logger.error("Routing failed provider=osrm elapsedMs={} message={}", elapsedMs, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            logger.error("Routing failed provider=osrm elapsedMs={} message={}", elapsedMs, ex.getMessage(), ex);
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider unavailable");
        }
    }

    private DirectionsResponse parseOsrmDirections(String body) {
        if (body == null || body.isBlank()) {
            logger.error("Routing parse failed provider=osrm reason=empty_body");
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider unavailable");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                logger.error("Routing parse failed provider=osrm reason=no_routes");
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider unavailable");
            }

            JsonNode firstRoute = routes.path(0);
            JsonNode coordinates = firstRoute.path("geometry").path("coordinates");
            List<DirectionsResponse.RoutePoint> routePoints = parseCoordinates(coordinates);
            if (routePoints.size() < 2) {
                logger.error("Routing parse failed provider=osrm reason=insufficient_geometry");
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider unavailable");
            }

            JsonNode firstLeg = firstRoute.path("legs").isArray() && !firstRoute.path("legs").isEmpty()
                    ? firstRoute.path("legs").path(0)
                    : null;

            double distanceMeters = firstLeg != null
                    ? firstLeg.path("distance").asDouble(firstRoute.path("distance").asDouble(0))
                    : firstRoute.path("distance").asDouble(0);
            double durationSeconds = firstLeg != null
                    ? firstLeg.path("duration").asDouble(firstRoute.path("duration").asDouble(0))
                    : firstRoute.path("duration").asDouble(0);

            List<DirectionsResponse.RouteStep> steps = parseOsrmSteps(firstLeg == null ? null : firstLeg.path("steps"));
            return toResponse(routePoints, distanceMeters, durationSeconds, steps);
        } catch (UpstreamServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Routing parse failed provider=osrm reason=unexpected message={}", ex.getMessage(), ex);
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider unavailable");
        }
    }

    private List<DirectionsResponse.RoutePoint> parseCoordinates(JsonNode coords) {
        List<DirectionsResponse.RoutePoint> route = new ArrayList<>();
        if (!coords.isArray()) {
            return route;
        }
        for (JsonNode coordinate : coords) {
            if (!coordinate.isArray() || coordinate.size() < 2) {
                continue;
            }
            route.add(new DirectionsResponse.RoutePoint(
                    coordinate.path(1).asDouble(),
                    coordinate.path(0).asDouble()
            ));
        }
        return route;
    }

    private List<DirectionsResponse.RouteStep> parseOsrmSteps(JsonNode stepsNode) {
        List<DirectionsResponse.RouteStep> steps = new ArrayList<>();
        if (stepsNode == null || !stepsNode.isArray()) {
            return steps;
        }

        for (JsonNode step : stepsNode) {
            JsonNode maneuver = step.path("maneuver");
            String instruction = maneuver.path("instruction").asText(null);
            if (instruction == null || instruction.isBlank()) {
                instruction = step.path("name").asText("Continue");
            }

            Integer maneuverType = null;
            JsonNode maneuverTypeNode = maneuver.path("type");
            if (maneuverTypeNode.isInt()) {
                maneuverType = maneuverTypeNode.asInt();
            }

            steps.add(new DirectionsResponse.RouteStep(
                    instruction,
                    step.path("distance").asDouble(0),
                    step.path("duration").asDouble(0),
                    maneuverType
            ));
        }
        return steps;
    }

    private DirectionsResponse toResponse(
            List<DirectionsResponse.RoutePoint> route,
            double distanceMeters,
            double durationSeconds,
            List<DirectionsResponse.RouteStep> steps
    ) {
        long etaSeconds = Math.round(durationSeconds);
        return new DirectionsResponse(route, distanceMeters, durationSeconds, etaSeconds, formatEta(etaSeconds), steps);
    }

    private String formatEta(long etaSeconds) {
        long mins = Math.max(1, Math.round(etaSeconds / 60.0));
        if (mins < 60) {
            return mins + " min";
        }
        long hours = mins / 60;
        long remMins = mins % 60;
        if (remMins == 0) {
            return hours + " hr";
        }
        return hours + " hr " + remMins + " min";
    }

    private void validateRequest(DirectionsRequest request) {
        if (request == null
                || request.getFromLat() == null
                || request.getFromLng() == null
                || request.getToLat() == null
                || request.getToLng() == null) {
            throw new IllegalArgumentException("Origin and destination coordinates are required.");
        }
    }

    private String cacheKey(DirectionsRequest request) {
        return String.format(
                Locale.US,
                "%.6f,%.6f:%.6f,%.6f",
                request.getFromLat(),
                request.getFromLng(),
                request.getToLat(),
                request.getToLng()
        );
    }

    private DirectionsResponse getCachedRoute(String key) {
        CacheEntry entry = routeCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMs < System.currentTimeMillis()) {
            routeCache.remove(key, entry);
            return null;
        }
        return entry.response;
    }

    @Transactional(readOnly = true)
    public RoutingStatsResponse getStats() {
        return new RoutingStatsResponse(
                inboundRequests.get(),
                outboundRequests.get(),
                cacheHits.get(),
                0L,
                0,
                routeCache.size()
        );
    }

    private static class CacheEntry {
        private final DirectionsResponse response;
        private final long expiresAtMs;

        private CacheEntry(DirectionsResponse response, long expiresAtMs) {
            this.response = response;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
