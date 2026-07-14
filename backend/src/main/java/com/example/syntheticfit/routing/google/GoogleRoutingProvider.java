package com.example.syntheticfit.routing.google;

import com.example.syntheticfit.routing.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;

/**
 * Google Directions API provider — not registered as a Spring bean.
 * Instantiate manually via RoutingConfiguration if GOOGLE_MAPS_API_KEY is set.
 */
public class GoogleRoutingProvider implements RoutingProvider {

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final RouteProcessor routeProcessor;
    private final HttpClient httpClient;

    public GoogleRoutingProvider(
            String apiKey,
            ObjectMapper objectMapper,
            RouteProcessor routeProcessor) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.routeProcessor = routeProcessor;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Route calculateRoute(RouteRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GOOGLE_MAPS_API_KEY is not configured");
        }

        String origin = request.start().latitude() + "," + request.start().longitude();
        String destination = request.end().latitude() + "," + request.end().longitude();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/directions/json")
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("mode", "bicycling")
                .queryParam("key", apiKey);

        if (request.waypoints() != null && !request.waypoints().isEmpty()) {
            String waypoints = request.waypoints().stream()
                    .map(c -> c.latitude() + "," + c.longitude())
                    .collect(Collectors.joining("|"));
            builder.queryParam("waypoints", waypoints);
        }

        URI uri = builder.build(true).toUri();

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder().uri(uri).GET().build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            GoogleRouteResponse parsed = objectMapper.readValue(response.body(), GoogleRouteResponse.class);

            if (!"OK".equals(parsed.status())) {
                throw new RuntimeException("Google Directions API returned status: " + parsed.status());
            }
            if (parsed.routes() == null || parsed.routes().isEmpty()) {
                throw new RuntimeException("No routes found");
            }

            String encoded = parsed.routes().get(0).overview_polyline().points();
            double[][] coords = GeoUtils.decodePolyline(encoded);
            return routeProcessor.buildRoute(coords, encoded);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to call Google Directions API", e);
        }
    }
}
