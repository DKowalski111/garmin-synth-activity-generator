package com.example.syntheticfit.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteApiRequest(
        @NotNull CoordinateDto start,
        @NotNull CoordinateDto end,
        List<CoordinateDto> waypoints
) {
    public record CoordinateDto(double latitude, double longitude) {}
}
