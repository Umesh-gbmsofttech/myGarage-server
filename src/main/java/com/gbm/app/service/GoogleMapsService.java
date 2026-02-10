package com.gbm.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbm.app.config.ApiKeysConfig;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleMapsService {

    private final ApiKeysConfig apiKeysConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleMapsService(ApiKeysConfig apiKeysConfig, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.apiKeysConfig = apiKeysConfig;
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> geocode(String address) {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("address", address)
                .queryParam("key", apiKeysConfig.getGoogleMapsKey())
                .toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return parseLocation(response.getBody());
    }

    public Map<String, Object> reverseGeocode(double lat, double lng) {
        String latlng = lat + "," + lng;
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("latlng", latlng)
                .queryParam("key", apiKeysConfig.getGoogleMapsKey())
                .toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return parseLocation(response.getBody());
    }

    public Map<String, Object> directions(String origin, String destination) {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/directions/json")
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("key", apiKeysConfig.getGoogleMapsKey())
                .toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return parseDirections(response.getBody());
    }

    private Map<String, Object> parseLocation(String body) {
        Map<String, Object> result = new HashMap<>();
        if (body == null || body.isBlank()) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode first = root.path("results").path(0);
            if (first.isMissingNode()) {
                return result;
            }
            JsonNode location = first.path("geometry").path("location");
            result.put("formattedAddress", first.path("formatted_address").asText(""));
            result.put("lat", location.path("lat").asDouble());
            result.put("lng", location.path("lng").asDouble());
        } catch (Exception ignored) {
        }
        return result;
    }

    private Map<String, Object> parseDirections(String body) {
        Map<String, Object> result = new HashMap<>();
        if (body == null || body.isBlank()) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode route = root.path("routes").path(0);
            JsonNode leg = route.path("legs").path(0);
            result.put("overviewPolyline", route.path("overview_polyline").path("points").asText(""));
            result.put("distanceText", leg.path("distance").path("text").asText(""));
            result.put("durationText", leg.path("duration").path("text").asText(""));
        } catch (Exception ignored) {
        }
        return result;
    }
}
