package com.example.syntheticfit.activity.generation;

import java.util.List;
import com.example.syntheticfit.activity.domain.PauseDefinition;

public record SpeedProfileRequest(
        double routeDistanceMeters,
        double averageSpeedMps,
        double movingDurationSeconds,
        int recordingIntervalSeconds,
        long seed,
        List<PauseDefinition> pauses
) {}
