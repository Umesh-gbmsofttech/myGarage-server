package com.gbm.app.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "api.keys")
public class ApiKeysConfig {

    @NotBlank
    private String googleMaps;

    @NotBlank
    private String gemini;

    public String getGoogleMaps() {
        return googleMaps;
    }

    public void setGoogleMaps(String googleMaps) {
        this.googleMaps = googleMaps;
    }

    public String getGemini() {
        return gemini;
    }

    public void setGemini(String gemini) {
        this.gemini = gemini;
    }

    public String getGoogleMapsKey() {
        return googleMaps;
    }

    public String getGeminiKey() {
        return gemini;
    }
}
