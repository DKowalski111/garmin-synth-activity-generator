package com.example.syntheticfit.activity.domain;

import java.time.Duration;

public record PauseDefinition(Duration offsetFromStart, Duration duration) {
    public PauseDefinition {
        if (offsetFromStart == null || offsetFromStart.isNegative()) {
            throw new IllegalArgumentException("Pause offset must be non-negative");
        }
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Pause duration must be positive");
        }
    }
}
