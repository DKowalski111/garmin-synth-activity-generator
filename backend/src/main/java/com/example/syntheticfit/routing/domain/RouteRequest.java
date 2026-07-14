package com.example.syntheticfit.routing.domain;

import java.util.List;

public record RouteRequest(
        Coordinate start,
        Coordinate end,
        List<Coordinate> waypoints
) {}
