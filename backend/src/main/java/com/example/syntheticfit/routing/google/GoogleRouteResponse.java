package com.example.syntheticfit.routing.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleRouteResponse(
        String status,
        List<GoogleRouteResult> routes
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleRouteResult(
            GoogleOverviewPolyline overview_polyline,
            List<GoogleLeg> legs
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleOverviewPolyline(String points) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleLeg(GoogleValue distance, GoogleValue duration) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleValue(int value, String text) {}
}
