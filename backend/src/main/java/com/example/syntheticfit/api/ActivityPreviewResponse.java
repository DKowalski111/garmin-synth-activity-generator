package com.example.syntheticfit.api;

import java.time.Instant;
import java.util.List;

public record ActivityPreviewResponse(
        String sport,
        SummaryDto summary,
        List<SampleDto> samples
) {
    public record SummaryDto(
            Instant startTime,
            Instant endTime,
            double distanceMeters,
            double movingDurationSeconds,
            double elapsedDurationSeconds,
            double totalPauseDurationSeconds,
            double averageSpeedMps,
            double maxSpeedMps,
            int averageHeartRate,
            int maxHeartRate,
            int sampleCount,
            int totalCalories
    ) {}

    public record SampleDto(
            Instant timestamp,
            double latitude,
            double longitude,
            double distanceMeters,
            double speedMetersPerSecond,
            int heartRate,
            boolean isPaused,
            Integer cadenceSpm
    ) {}
}
