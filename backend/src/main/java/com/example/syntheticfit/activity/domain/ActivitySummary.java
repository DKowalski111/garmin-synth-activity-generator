package com.example.syntheticfit.activity.domain;

import java.time.Instant;

public record ActivitySummary(
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
