package com.gbm.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbm.app.config.ApiKeysConfig;
import com.gbm.app.dto.DirectionsRequest;
import com.gbm.app.dto.DirectionsResponse;
import com.gbm.app.dto.RoutingStatsResponse;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OpenRouteService {

    private static final String ORS_DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private final ApiKeysConfig apiKeysConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CacheEntry> routeCache = new ConcurrentHashMap<>();
    private final AtomicLong inboundRequests = new AtomicLong(0);
    private final AtomicLong outboundRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);

    public OpenRouteService(ApiKeysConfig apiKeysConfig, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.apiKeysConfig = apiKeysConfig;
        this.restTemplate = restTemplateBuilder.build();
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

        DirectionsResponse fresh = fetchRouteFromOrs(request);
        routeCache.put(cacheKey, new CacheEntry(fresh, System.currentTimeMillis() + CACHE_TTL_MS));
        return fresh;
    }

    private DirectionsResponse fetchRouteFromOrs(DirectionsRequest request) {
        String start = request.getOriginLng() + "," + request.getOriginLat();
        String end = request.getDestinationLng() + "," + request.getDestinationLat();
        String url = UriComponentsBuilder.fromHttpUrl(ORS_DIRECTIONS_URL)
                .queryParam("api_key", apiKeysConfig.getOpenRouteServiceKey())
                .queryParam("start", start)
                .queryParam("end", end)
                .toUriString();

        outboundRequests.incrementAndGet();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return parseDirections(response.getBody());
    }

    private DirectionsResponse parseDirections(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("No route returned by OpenRouteService.");
        }

        try {
            JsonNode root = objectMapper.readTree(body);

            if (root.path("features").isArray() && !root.path("features").isEmpty()) {
                return parseGeoJsonRoute(root.path("features").path(0));
            }
            if (root.path("routes").isArray() && !root.path("routes").isEmpty()) {
                return parseStandardRoute(root.path("routes").path(0));
            }
            throw new IllegalArgumentException("No route found for the provided coordinates.");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse OpenRouteService response.", ex);
        }
    }

    private DirectionsResponse parseGeoJsonRoute(JsonNode feature) {
        JsonNode coords = feature.path("geometry").path("coordinates");
        JsonNode summary = feature.path("properties").path("summary");
        List<DirectionsResponse.RoutePoint> route = parseCoordinates(coords);
        if (route.isEmpty()) {
            throw new IllegalArgumentException("No valid route points returned by OpenRouteService.");
        }
        return toResponse(route, summary.path("distance").asDouble(0), summary.path("duration").asDouble(0));
    }

    private DirectionsResponse parseStandardRoute(JsonNode routeNode) {
        JsonNode summary = routeNode.path("summary");
        List<DirectionsResponse.RoutePoint> routePoints = parseRouteGeometry(routeNode.path("geometry"));
        if (routePoints.isEmpty()) {
            throw new IllegalArgumentException("No valid route geometry returned by OpenRouteService.");
        }
        return toResponse(routePoints, summary.path("distance").asDouble(0), summary.path("duration").asDouble(0));
    }

    private List<DirectionsResponse.RoutePoint> parseRouteGeometry(JsonNode geometryNode) {
        if (geometryNode.isArray()) {
            return parseCoordinates(geometryNode);
        }
        if (geometryNode.isTextual()) {
            return decodePolyline(geometryNode.asText());
        }
        JsonNode coordinates = geometryNode.path("coordinates");
        if (coordinates.isArray()) {
            return parseCoordinates(coordinates);
        }
        return List.of();
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

    private List<DirectionsResponse.RoutePoint> decodePolyline(String polyline) {
        List<DirectionsResponse.RoutePoint> points = new ArrayList<>();
        if (polyline == null || polyline.isBlank()) {
            return points;
        }

        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < polyline.length()) {
            int shift = 0;
            int result = 0;
            int b;
            do {
                b = polyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < polyline.length());
            int dLat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lat += dLat;

            shift = 0;
            result = 0;
            do {
                b = polyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < polyline.length());
            int dLng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lng += dLng;

            points.add(new DirectionsResponse.RoutePoint(lat / 1e5, lng / 1e5));
        }
        return points;
    }

    private DirectionsResponse toResponse(List<DirectionsResponse.RoutePoint> route, double distanceMeters, double durationSeconds) {
        long etaSeconds = Math.round(durationSeconds);
        return new DirectionsResponse(route, distanceMeters, durationSeconds, etaSeconds, formatEta(etaSeconds));
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
                || request.getOriginLat() == null
                || request.getOriginLng() == null
                || request.getDestinationLat() == null
                || request.getDestinationLng() == null) {
            throw new IllegalArgumentException("Origin and destination coordinates are required.");
        }
    }

    private String cacheKey(DirectionsRequest request) {
        return String.format(
                Locale.US,
                "%.6f,%.6f:%.6f,%.6f",
                request.getOriginLat(),
                request.getOriginLng(),
                request.getDestinationLat(),
                request.getDestinationLng()
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
