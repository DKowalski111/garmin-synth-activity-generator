package com.example.syntheticfit.api;

import java.util.List;

public record RouteApiResponse(
        double distanceMeters,
        String encodedPolyline,
        List<PointDto> points
) {
    public record PointDto(double latitude, double longitude) {}
}
