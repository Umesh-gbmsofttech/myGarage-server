package com.gbm.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
@AllArgsConstructor
public class LiveLocationDTO {
    private Long bookingId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedAt;

    public LiveLocationDTO(Long bookingId, Double latitude, Double longitude, Instant updatedAt) {
        this.bookingId = bookingId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = updatedAt == null ? null : LocalDateTime.ofInstant(updatedAt, ZoneOffset.UTC);
    }

    public static LiveLocationDTO of(Long bookingId, Double latitude, Double longitude, Instant updatedAt) {
        LocalDateTime updated = updatedAt == null ? null : LocalDateTime.ofInstant(updatedAt, ZoneOffset.UTC);
        return new LiveLocationDTO(bookingId, latitude, longitude, updated);
    }
}
