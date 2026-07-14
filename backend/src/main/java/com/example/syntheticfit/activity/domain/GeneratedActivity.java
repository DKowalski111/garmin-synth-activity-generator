package com.example.syntheticfit.activity.domain;

import java.util.List;

public record GeneratedActivity(
        ActivitySummary summary,
        List<ActivitySample> samples,
        SportType sport
) {}
