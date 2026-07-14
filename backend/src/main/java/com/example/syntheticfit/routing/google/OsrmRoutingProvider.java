package com.example.syntheticfit.routing.google;

import com.example.syntheticfit.routing.domain.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Routes via the OSRM public demo server (router.project-osrm.org).
 * No API key required. Uses the bicycle profile.
 * For production use, run your own OSRM instance.
 */
public class OsrmRoutingProvider implements RoutingProvider {

    private static final String OSRM_BASE = "https://router.project-osrm.org/route/v1/bike";

    private final ObjectMapper objectMapper;
    private final RouteProcessor routeProcessor;
    private final HttpClient httpClient;

    public OsrmRoutingProvider(ObjectMapper objectMapper, RouteProcessor routeProcessor) {
        this.objectMapper = objectMapper;
        this.routeProcessor = routeProcessor;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Route calculateRoute(RouteRequest request) {
        String coords = buildCoordinateString(request);
        String url = OSRM_BASE + "/" + coords + "?overview=full&geometries=geojson";

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "synthetic-fit-generator/1.0 (local dev tool)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("OSRM returned HTTP " + response.statusCode());
            }

            OsrmResponse parsed = objectMapper.readValue(response.body(), OsrmResponse.class);

            if (!"Ok".equals(parsed.code())) {
                throw new RuntimeException("OSRM error: " + parsed.code() + " — " + parsed.message());
            }
            if (parsed.routes() == null || parsed.routes().isEmpty()) {
                throw new RuntimeException("OSRM returned no routes");
            }

            OsrmRoute route = parsed.routes().get(0);
            // GeoJSON coordinates are [longitude, latitude]
            List<double[]> geoJsonCoords = route.geometry().coordinates();
            double[][] coords2d = geoJsonCoords.stream()
                    .map(c -> new double[]{c[1], c[0]}) // flip to [lat, lon]
                    .toArray(double[][]::new);

            return routeProcessor.buildRoute(coords2d, "");

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to call OSRM routing API", e);
        }
    }

    private String buildCoordinateString(RouteRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.start().longitude()).append(",").append(request.start().latitude());
        if (request.waypoints() != null) {
            for (Coordinate wp : request.waypoints()) {
                sb.append(";").append(wp.longitude()).append(",").append(wp.latitude());
            }
        }
        sb.append(";").append(request.end().longitude()).append(",").append(request.end().latitude());
        return sb.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmResponse(String code, String message, List<OsrmRoute> routes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmRoute(double distance, double duration, OsrmGeometry geometry) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmGeometry(List<double[]> coordinates) {}
}
