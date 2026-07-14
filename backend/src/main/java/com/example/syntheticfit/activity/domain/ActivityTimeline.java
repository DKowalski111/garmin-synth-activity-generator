package com.example.syntheticfit.activity.domain;

import java.time.Instant;

public record ActivityTimeline(
        Instant activityStart,
        Instant activityEnd,
        double movingDurationSeconds,
        double elapsedDurationSeconds,
        double totalPauseDurationSeconds
) {}
