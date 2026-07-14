package com.example.syntheticfit.configuration;

import com.example.syntheticfit.routing.domain.RoutingProvider;
import com.example.syntheticfit.routing.domain.RouteProcessor;
import com.example.syntheticfit.routing.google.GoogleRoutingProvider;
import com.example.syntheticfit.routing.google.ValhallaRoutingProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingConfiguration {

    @Bean
    public RoutingProvider routingProvider(
            AppProperties properties,
            ObjectMapper objectMapper,
            RouteProcessor routeProcessor) {

        String googleKey = properties.getGoogle().getMapsApiKey();
        if (googleKey != null && !googleKey.isBlank()) {
            return new GoogleRoutingProvider(googleKey, objectMapper, routeProcessor);
        }
        return new ValhallaRoutingProvider(objectMapper, routeProcessor);
    }
}
