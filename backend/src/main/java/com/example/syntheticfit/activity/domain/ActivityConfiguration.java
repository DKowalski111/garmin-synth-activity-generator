package com.example.syntheticfit.activity.domain;

import java.util.List;

public record ActivityConfiguration(
        String activityName,
        double averageSpeedKmh,
        int averageHeartRate,
        int recordingIntervalSeconds,
        long seed,
        TimeConfiguration timeConfiguration,
        List<PauseDefinition> pauses
) {
    public ActivityConfiguration {
        if (averageSpeedKmh < 1 || averageSpeedKmh > 100) {
            throw new IllegalArgumentException("Average speed must be between 1 and 100 km/h");
        }
        if (averageHeartRate < 40 || averageHeartRate > 220) {
            throw new IllegalArgumentException("Average heart rate must be between 40 and 220 BPM");
        }
        if (recordingIntervalSeconds < 1 || recordingIntervalSeconds > 60) {
            throw new IllegalArgumentException("Recording interval must be between 1 and 60 seconds");
        }
        if (pauses == null) pauses = List.of();
    }
}
