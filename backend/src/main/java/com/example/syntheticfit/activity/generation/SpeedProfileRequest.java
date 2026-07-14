package com.example.syntheticfit.activity.generation;

import com.example.syntheticfit.activity.domain.PauseDefinition;
import com.example.syntheticfit.activity.domain.SportType;

import java.util.List;

public record SpeedProfileRequest(
        double routeDistanceMeters,
        double averageSpeedMps,
        double movingDurationSeconds,
        int recordingIntervalSeconds,
        long seed,
        List<PauseDefinition> pauses,
        SportType sport
) {}
