package com.gbm.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "api.keys")
public class ApiKeysConfig {

    @NotBlank
    private String openAi;

    @NotBlank
    private String openRouteService;

    @Valid
    private MapConfig map = new MapConfig();

    public String getOpenAiKey() {
        return openAi;
    }

    public String getMapTilerKey() {
        return map.getTiler();
    }

    public String getOpenRouteServiceKey() {
        return openRouteService;
    }

    public String getOpenAi() {
        return openAi;
    }

    public void setOpenAi(String openAi) {
        this.openAi = openAi;
    }

    public String getOpenRouteService() {
        return openRouteService;
    }

    public void setOpenRouteService(String openRouteService) {
        this.openRouteService = openRouteService;
    }

    public MapConfig getMap() {
        return map;
    }

    public void setMap(MapConfig map) {
        this.map = map;
    }

    public static class MapConfig {

        @NotBlank
        private String tiler;

        public String getTiler() {
            return tiler;
        }

        public void setTiler(String tiler) {
            this.tiler = tiler;
        }
    }
}
