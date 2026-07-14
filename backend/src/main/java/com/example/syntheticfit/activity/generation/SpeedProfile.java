package com.example.syntheticfit.activity.generation;

import java.util.List;

public record SpeedProfile(List<Double> speedsAtEachSample, double totalDistanceMeters) {}
