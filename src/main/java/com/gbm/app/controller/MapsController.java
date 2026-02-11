package com.gbm.app.controller;

import com.gbm.app.config.ApiKeysConfig;
import com.gbm.app.dto.DirectionsRequest;
import com.gbm.app.dto.DirectionsResponse;
import com.gbm.app.dto.MapConfigResponse;
import com.gbm.app.dto.RoutingStatsResponse;
import com.gbm.app.service.OpenRouteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maps")
public class MapsController {

    private final OpenRouteService openRouteService;
    private final ApiKeysConfig apiKeysConfig;

    public MapsController(OpenRouteService openRouteService, ApiKeysConfig apiKeysConfig) {
        this.openRouteService = openRouteService;
        this.apiKeysConfig = apiKeysConfig;
    }

    @GetMapping("/config")
    public ResponseEntity<MapConfigResponse> config() {
        String template = "https://api.maptiler.com/maps/streets/{z}/{x}/{y}.png?key=" + apiKeysConfig.getMapTilerKey();
        return ResponseEntity.ok(new MapConfigResponse(template));
    }

    @PostMapping("/directions")
    public ResponseEntity<DirectionsResponse> directions(@Valid @RequestBody DirectionsRequest request) {
        return ResponseEntity.ok(openRouteService.getDirections(request));
    }

    @GetMapping("/routing-stats")
    public ResponseEntity<RoutingStatsResponse> routingStats() {
        return ResponseEntity.ok(openRouteService.getStats());
    }
}
