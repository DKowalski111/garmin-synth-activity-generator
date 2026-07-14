package com.example.syntheticfit.routing.google;

import com.example.syntheticfit.routing.domain.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Routes via the public Valhalla instance at valhalla.openstreetmap.de.
 * No API key required. Uses the bicycle costing model with a strong
 * preference for dedicated cycling infrastructure (cycle lanes, paths,
 * sidewalks) over roads shared with motor traffic.
 */
public class ValhallaRoutingProvider implements RoutingProvider {

    private static final String VALHALLA_URL = "https://valhalla1.openstreetmap.de/route";

    private final ObjectMapper objectMapper;
    private final RouteProcessor routeProcessor;
    private final HttpClient httpClient;

    public ValhallaRoutingProvider(ObjectMapper objectMapper, RouteProcessor routeProcessor) {
        this.objectMapper = objectMapper;
        this.routeProcessor = routeProcessor;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Route calculateRoute(RouteRequest request) {
        String body = buildRequestBody(request);

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(VALHALLA_URL))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "synthetic-fit-generator/1.0 (local dev tool)")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Valhalla returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }

            ValhallaResponse parsed = objectMapper.readValue(response.body(), ValhallaResponse.class);

            if (parsed.trip() == null || parsed.trip().legs() == null || parsed.trip().legs().isEmpty()) {
                throw new RuntimeException("Valhalla returned no route");
            }

            // Concatenate all legs — Valhalla returns one leg per segment
            // (start→wp1, wp1→wp2, …, wpN→end). Each leg's shape is independent.
            List<double[]> allCoords = new ArrayList<>();
            for (ValhallaLeg leg : parsed.trip().legs()) {
                double[][] legCoords = decodePolyline6(leg.shape());
                // Skip the first point of subsequent legs — it duplicates the last point of the previous leg
                int startIdx = allCoords.isEmpty() ? 0 : 1;
                for (int i = startIdx; i < legCoords.length; i++) {
                    allCoords.add(legCoords[i]);
                }
            }
            double[][] coords = allCoords.toArray(new double[0][2]);
            return routeProcessor.buildRoute(coords, "");

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to call Valhalla routing API", e);
        }
    }

    private String buildRequestBody(RouteRequest request) {
        List<Map<String, Double>> locations = new ArrayList<>();
        locations.add(Map.of("lon", request.start().longitude(), "lat", request.start().latitude()));
        if (request.waypoints() != null) {
            for (Coordinate wp : request.waypoints()) {
                locations.add(Map.of("lon", wp.longitude(), "lat", wp.latitude()));
            }
        }
        locations.add(Map.of("lon", request.end().longitude(), "lat", request.end().latitude()));

        // use_roads=0.1 strongly prefers cycling paths/lanes over shared roads
        Map<String, Object> bicycleCostingOptions = Map.of(
                "use_roads", 0.1,
                "use_hills", 0.3,
                "bicycle_type", "hybrid"
        );

        Map<String, Object> requestMap = Map.of(
                "locations", locations,
                "costing", "bicycle",
                "costing_options", Map.of("bicycle", bicycleCostingOptions),
                "units", "kilometers"
        );

        try {
            return objectMapper.writeValueAsString(requestMap);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize Valhalla request", e);
        }
    }

    /**
     * Decodes a Valhalla encoded polyline (precision 6 — 1e-6 degrees per unit).
     */
    private double[][] decodePolyline6(String encoded) {
        List<double[]> result = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, r = 0;
            do {
                b = encoded.charAt(index++) - 63;
                r |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((r & 1) != 0 ? ~(r >> 1) : (r >> 1));
            lat += dlat;
            shift = 0; r = 0;
            do {
                b = encoded.charAt(index++) - 63;
                r |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((r & 1) != 0 ? ~(r >> 1) : (r >> 1));
            lng += dlng;
            result.add(new double[]{lat * 1e-6, lng * 1e-6});
        }
        return result.toArray(new double[0][2]);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ValhallaResponse(ValhallaTrip trip) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ValhallaTrip(List<ValhallaLeg> legs, ValhallaSummary summary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ValhallaLeg(String shape, ValhallaSummary summary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ValhallaSummary(double length, double time) {}
}
