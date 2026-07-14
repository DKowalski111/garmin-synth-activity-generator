package com.example.syntheticfit.activity.domain;

import java.time.Instant;

public record ActivitySample(
        Instant timestamp,
        double latitude,
        double longitude,
        double distanceMeters,
        double speedMetersPerSecond,
        int heartRate,
        Double altitudeMeters,
        boolean isPaused,
        Integer cadenceSpm
) {}
