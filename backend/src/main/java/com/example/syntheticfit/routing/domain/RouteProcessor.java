package com.example.syntheticfit.routing.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RouteProcessor {

    /**
     * Takes raw decoded coordinate pairs and builds a Route with cumulative distances.
     */
    public Route buildRoute(double[][] coordinates, String encodedPolyline) {
        if (coordinates.length < 2) {
            throw new IllegalArgumentException("At least 2 coordinates required");
        }
        List<RoutePoint> points = new ArrayList<>();
        double cumulative = 0.0;
        points.add(new RoutePoint(coordinates[0][0], coordinates[0][1], 0.0));
        for (int i = 1; i < coordinates.length; i++) {
            cumulative += GeoUtils.distanceMeters(
                    coordinates[i - 1][0], coordinates[i - 1][1],
                    coordinates[i][0], coordinates[i][1]);
            points.add(new RoutePoint(coordinates[i][0], coordinates[i][1], cumulative));
        }
        return new Route(List.copyOf(points), cumulative, encodedPolyline != null ? encodedPolyline : "");
    }

    /**
     * Given a route and a traveled distance, interpolates the lat/lon position.
     */
    public double[] interpolatePosition(Route route, double distanceMeters) {
        List<RoutePoint> points = route.points();
        if (distanceMeters <= 0) {
            return new double[]{points.get(0).latitude(), points.get(0).longitude()};
        }
        if (distanceMeters >= route.distanceMeters()) {
            RoutePoint last = points.get(points.size() - 1);
            return new double[]{last.latitude(), last.longitude()};
        }
        for (int i = 1; i < points.size(); i++) {
            RoutePoint prev = points.get(i - 1);
            RoutePoint curr = points.get(i);
            if (distanceMeters <= curr.cumulativeDistanceMeters()) {
                double segLen = curr.cumulativeDistanceMeters() - prev.cumulativeDistanceMeters();
                double t = segLen > 0 ? (distanceMeters - prev.cumulativeDistanceMeters()) / segLen : 0.0;
                return GeoUtils.interpolate(prev.latitude(), prev.longitude(), curr.latitude(), curr.longitude(), t);
            }
        }
        RoutePoint last = points.get(points.size() - 1);
        return new double[]{last.latitude(), last.longitude()};
    }
}
