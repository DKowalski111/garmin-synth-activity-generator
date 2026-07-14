package com.example.syntheticfit.routing.domain;

import com.example.syntheticfit.activity.domain.SportType;

import java.util.List;

public record RouteRequest(
        Coordinate start,
        Coordinate end,
        List<Coordinate> waypoints,
        SportType sport
) {}
