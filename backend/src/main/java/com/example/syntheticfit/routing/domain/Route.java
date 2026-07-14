package com.example.syntheticfit.routing.domain;

import java.util.List;

public record Route(
        List<RoutePoint> points,
        double distanceMeters,
        String encodedPolyline
) {
    public Route {
        if (points == null || points.size() < 2) throw new IllegalArgumentException("Route must have at least 2 points");
        if (distanceMeters <= 0) throw new IllegalArgumentException("Route distance must be positive");
    }
}
