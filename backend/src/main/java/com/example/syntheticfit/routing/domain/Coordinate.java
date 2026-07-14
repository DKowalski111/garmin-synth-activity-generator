package com.example.syntheticfit.routing.domain;

public record Coordinate(double latitude, double longitude) {
    public Coordinate {
        if (latitude < -90 || latitude > 90) throw new IllegalArgumentException("Invalid latitude: " + latitude);
        if (longitude < -180 || longitude > 180) throw new IllegalArgumentException("Invalid longitude: " + longitude);
    }
}
