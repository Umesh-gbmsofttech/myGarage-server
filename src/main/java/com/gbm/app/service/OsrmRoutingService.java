package com.gbm.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbm.app.config.ApiKeysConfig;
import com.gbm.app.dto.DirectionsRequest;
import com.gbm.app.dto.DirectionsResponse;
import com.gbm.app.dto.RoutingStatsResponse;
import com.gbm.app.exception.UpstreamServiceException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OsrmRoutingService {

    private static final String ORS_DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
    private static final String PUBLIC_OSRM_URL = "https://router.project-osrm.org/route/v1/driving/";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private final ApiKeysConfig apiKeysConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CacheEntry> routeCache = new ConcurrentHashMap<>();
    private final AtomicLong inboundRequests = new AtomicLong(0);
    private final AtomicLong outboundRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);

    public OsrmRoutingService(ApiKeysConfig apiKeysConfig, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.apiKeysConfig = apiKeysConfig;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(4))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
        this.objectMapper = objectMapper;
    }

    public DirectionsResponse getDirections(DirectionsRequest request) {
        validateRequest(request);
        inboundRequests.incrementAndGet();

        String cacheKey = cacheKey(request);
        DirectionsResponse cached = getCachedRoute(cacheKey);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }

        DirectionsResponse fresh = fetchRouteWithFallback(request);
        routeCache.put(cacheKey, new CacheEntry(fresh, System.currentTimeMillis() + CACHE_TTL_MS));
        return fresh;
    }

    private DirectionsResponse fetchRouteWithFallback(DirectionsRequest request) {
        try {
            return fetchRouteFromOpenRouteService(request);
        } catch (UpstreamServiceException ex) {
            try {
                return fetchRouteFromPublicOsrm(request);
            } catch (UpstreamServiceException fallbackEx) {
                throw ex;
            }
        }
    }

    private DirectionsResponse fetchRouteFromOpenRouteService(DirectionsRequest request) {
        String start = String.format(
                Locale.US,
                "%.6f,%.6f",
                request.getFromLng(),
                request.getFromLat()
        );
        String end = String.format(
                Locale.US,
                "%.6f,%.6f",
                request.getToLng(),
                request.getToLat()
        );
        String url = UriComponentsBuilder.fromHttpUrl(ORS_DIRECTIONS_URL)
                .queryParam("api_key", apiKeysConfig.getOpenRouteServiceKey())
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("instructions", true)
                .build()
                .encode()
                .toUriString();

        try {
            outboundRequests.incrementAndGet();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return parseDirections(response.getBody());
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw new UpstreamServiceException(HttpStatus.GATEWAY_TIMEOUT, "Routing provider timeout.");
            }
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider unavailable: " + safeMessage(ex.getMessage()));
        } catch (HttpStatusCodeException ex) {
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider error: " + extractUpstreamError(ex));
        }
    }

    private DirectionsResponse fetchRouteFromPublicOsrm(DirectionsRequest request) {
        String start = String.format(Locale.US, "%.6f,%.6f", request.getFromLng(), request.getFromLat());
        String end = String.format(Locale.US, "%.6f,%.6f", request.getToLng(), request.getToLat());
        String url = PUBLIC_OSRM_URL + start + ";" + end + "?overview=full&geometries=geojson";
        try {
            outboundRequests.incrementAndGet();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return parseOsrmDirections(response.getBody());
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw new UpstreamServiceException(HttpStatus.GATEWAY_TIMEOUT, "Routing provider timeout.");
            }
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider unavailable: " + safeMessage(ex.getMessage()));
        } catch (HttpStatusCodeException ex) {
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Routing provider error: " + extractUpstreamError(ex));
        }
    }

    private boolean isTimeout(ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        String message = ex.getMessage();
        return (cause != null && cause.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("timeout"))
                || (message != null && message.toLowerCase(Locale.ROOT).contains("timed out"));
    }

    private DirectionsResponse parseDirections(String body) {
        if (body == null || body.isBlank()) {
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Invalid routing response.");
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "No route found for the provided coordinates.");
            }

            JsonNode feature = features.path(0);
            JsonNode coordinates = feature.path("geometry").path("coordinates");
            List<DirectionsResponse.RoutePoint> routePoints = parseCoordinates(coordinates);
            if (routePoints.isEmpty()) {
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Invalid routing response.");
            }

            JsonNode summary = feature.path("properties").path("summary");
            double distanceMeters = summary.path("distance").asDouble(0);
            double durationSeconds = summary.path("duration").asDouble(0);
            List<DirectionsResponse.RouteStep> steps = parseSteps(feature.path("properties").path("segments"));
            return toResponse(routePoints, distanceMeters, durationSeconds, steps);
        } catch (UpstreamServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Invalid routing response.");
        }
    }

    private DirectionsResponse parseOsrmDirections(String body) {
        if (body == null || body.isBlank()) {
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Invalid routing response.");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "No route found for the provided coordinates.");
            }
            JsonNode firstRoute = routes.path(0);
            JsonNode coordinates = firstRoute.path("geometry").path("coordinates");
            List<DirectionsResponse.RoutePoint> routePoints = parseCoordinates(coordinates);
            if (routePoints.isEmpty()) {
                throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Invalid routing response.");
            }
            double distanceMeters = firstRoute.path("distance").asDouble(0);
            double durationSeconds = firstRoute.path("duration").asDouble(0);
            return toResponse(routePoints, distanceMeters, durationSeconds, new ArrayList<>());
        } catch (UpstreamServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Invalid routing response.");
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

    private List<DirectionsResponse.RouteStep> parseSteps(JsonNode segmentsNode) {
        List<DirectionsResponse.RouteStep> steps = new ArrayList<>();
        if (!segmentsNode.isArray() || segmentsNode.isEmpty()) {
            return steps;
        }
        JsonNode firstSegment = segmentsNode.path(0);
        JsonNode stepNodes = firstSegment.path("steps");
        if (!stepNodes.isArray()) {
            return steps;
        }
        for (JsonNode step : stepNodes) {
            steps.add(new DirectionsResponse.RouteStep(
                    step.path("instruction").asText("Continue"),
                    step.path("distance").asDouble(0),
                    step.path("duration").asDouble(0),
                    step.path("type").isNumber() ? step.path("type").asInt() : null
            ));
        }
        return steps;
    }

    private DirectionsResponse toResponse(List<DirectionsResponse.RoutePoint> route, double distanceMeters, double durationSeconds,
                                          List<DirectionsResponse.RouteStep> steps) {
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

    private String extractUpstreamError(HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(body);
                String message = node.path("error").asText(null);
                if (message == null || message.isBlank()) {
                    message = node.path("message").asText(null);
                }
                if (message != null && !message.isBlank()) {
                    return message;
                }
            } catch (Exception ignored) {
                return body;
            }
            return body;
        }
        return ex.getStatusCode().toString();
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        return message;
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
