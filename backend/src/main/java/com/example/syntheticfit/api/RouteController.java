package com.example.syntheticfit.api;

import com.example.syntheticfit.routing.domain.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RoutingProvider routingProvider;

    public RouteController(RoutingProvider routingProvider) {
        this.routingProvider = routingProvider;
    }

    @PostMapping
    public ResponseEntity<RouteApiResponse> calculateRoute(@Valid @RequestBody RouteApiRequest request) {
        RouteRequest routeRequest = new RouteRequest(
                new Coordinate(request.start().latitude(), request.start().longitude()),
                new Coordinate(request.end().latitude(), request.end().longitude()),
                request.waypoints() == null ? List.of() :
                        request.waypoints().stream()
                                .map(w -> new Coordinate(w.latitude(), w.longitude()))
                                .toList()
        );

        Route route = routingProvider.calculateRoute(routeRequest);

        List<RouteApiResponse.PointDto> points = route.points().stream()
                .map(p -> new RouteApiResponse.PointDto(p.latitude(), p.longitude()))
                .toList();

        return ResponseEntity.ok(new RouteApiResponse(route.distanceMeters(), route.encodedPolyline(), points));
    }
}
