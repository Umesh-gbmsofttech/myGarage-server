package com.gbm.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DirectionsRequest {

    @NotNull
    private Double originLat;

    @NotNull
    private Double originLng;

    @NotNull
    private Double destinationLat;

    @NotNull
    private Double destinationLng;
}
