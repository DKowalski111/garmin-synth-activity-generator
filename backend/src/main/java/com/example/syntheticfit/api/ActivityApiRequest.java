package com.example.syntheticfit.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ActivityApiRequest(
        String activityName,
        String sport,
        @NotNull RouteDto route,
        @NotNull TimeConfigDto timeConfiguration,
        @DecimalMin("1.0") @DecimalMax("100.0") double averageSpeedKmh,
        @Min(40) @Max(220) int averageHeartRate,
        @Min(1) @Max(60) int recordingIntervalSeconds,
        long seed,
        List<PauseDto> pauses,
        Instant fixedEndTime,
        // Running-specific fields (null for cycling)
        Double averagePaceMinPerKm,
        Integer cadenceSpm
) {
    public record RouteDto(double distanceMeters, List<PointDto> points) {}
    public record PointDto(double latitude, double longitude) {}
    public record TimeConfigDto(String mode, Instant selectedTime) {}
    public record PauseDto(long offsetSeconds, long durationSeconds) {}
}
