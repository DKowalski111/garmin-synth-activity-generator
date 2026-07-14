package com.example.syntheticfit.routing.domain;

public interface RoutingProvider {
    Route calculateRoute(RouteRequest request);
}
