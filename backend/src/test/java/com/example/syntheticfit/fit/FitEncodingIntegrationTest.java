package com.example.syntheticfit.fit;

import com.example.syntheticfit.activity.domain.*;
import com.example.syntheticfit.activity.generation.*;
import com.example.syntheticfit.activity.timeline.ActivityTimeCalculator;
import com.example.syntheticfit.activity.validation.PauseValidator;
import com.example.syntheticfit.fit.encoding.FitActivityEncoder;
import com.example.syntheticfit.fit.validation.FitValidationResult;
import com.example.syntheticfit.fit.validation.GarminFitActivityValidator;
import com.example.syntheticfit.routing.domain.Route;
import com.example.syntheticfit.routing.domain.RouteProcessor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FitEncodingIntegrationTest {

    private static final Instant FIXED_NOW = Instant.parse("2024-06-15T10:00:00Z");
    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private final RouteProcessor routeProcessor = new RouteProcessor();
    private final ActivityTimeCalculator timeCalc = new ActivityTimeCalculator(fixedClock);
    private final RealisticSpeedProfileGenerator speedGen = new RealisticSpeedProfileGenerator();
    private final HeartRateProfileGenerator hrGen = new HeartRateProfileGenerator();
    private final PauseValidator pauseValidator = new PauseValidator();
    private final ActivityGenerator generator = new ActivityGenerator(
            timeCalc, speedGen, hrGen, routeProcessor, pauseValidator);
    private final FitActivityEncoder encoder = new FitActivityEncoder();
    private final GarminFitActivityValidator validator = new GarminFitActivityValidator();

    @Test
    void generatedFitFile_passesValidation() {
        Route route = buildTestRoute();
        ActivityConfiguration config = new ActivityConfiguration(
                "Integration Test", SportType.CYCLING, 25.0, 145, 5, 42L,
                new TimeConfiguration(TimeMode.END_NOW, null), List.of(), null);

        GeneratedActivity activity = generator.generate(route, config);
        byte[] fit = encoder.encode(activity);

        assertThat(fit).isNotEmpty();
        assertThat(fit.length).isGreaterThan(14);

        FitValidationResult result = validator.validate(fit);
        assertThat(result.valid()).as("Validation issues: " + result.issues()).isTrue();
    }

    @Test
    void fitFile_containsRequiredMessages() {
        Route route = buildTestRoute();
        ActivityConfiguration config = new ActivityConfiguration(
                "Test", SportType.CYCLING, 25.0, 145, 10, 1L,
                new TimeConfiguration(TimeMode.END_NOW, null), List.of(), null);

        GeneratedActivity activity = generator.generate(route, config);
        byte[] fit = encoder.encode(activity);

        FitValidationResult result = validator.validate(fit);
        assertThat(result.valid()).as(result.issues().toString()).isTrue();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void fitFile_hasBinaryContent() {
        Route route = buildTestRoute();
        ActivityConfiguration config = new ActivityConfiguration(
                "Test", SportType.CYCLING, 20.0, 140, 5, 77L,
                new TimeConfiguration(TimeMode.END_AT_SELECTED_TIME,
                        Instant.parse("2024-06-15T09:00:00Z")),
                List.of(), null);

        GeneratedActivity activity = generator.generate(route, config);
        byte[] fit = encoder.encode(activity);

        // FIT files start with ".FIT" header marker
        assertThat(fit[8]).isEqualTo((byte) 0x2E); // '.'
        assertThat(fit[9]).isEqualTo((byte) 0x46);  // 'F'
        assertThat(fit[10]).isEqualTo((byte) 0x49); // 'I'
        assertThat(fit[11]).isEqualTo((byte) 0x54); // 'T'
    }

    private Route buildTestRoute() {
        double[][] coords = {
                {50.2945, 18.6712},
                {50.2950, 18.6800},
                {50.2960, 18.6900},
                {50.2970, 18.7000},
                {50.2981, 18.9974}
        };
        return routeProcessor.buildRoute(coords, "test");
    }
}
