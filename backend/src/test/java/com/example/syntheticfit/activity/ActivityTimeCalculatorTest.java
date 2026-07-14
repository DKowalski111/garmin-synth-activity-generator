package com.example.syntheticfit.activity;

import com.example.syntheticfit.activity.domain.*;
import com.example.syntheticfit.activity.timeline.ActivityTimeCalculator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ActivityTimeCalculatorTest {

    private static final Instant FIXED_NOW = Instant.parse("2024-06-15T10:00:00Z");
    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final ActivityTimeCalculator calculator = new ActivityTimeCalculator(fixedClock);

    @Test
    void endNow_setsEndToClockInstant() {
        ActivityConfiguration config = new ActivityConfiguration(
                "Test", 36.0, 150, 1, 42L,
                new TimeConfiguration(TimeMode.END_NOW, null), List.of());
        // 10000m at 10 m/s = 1000 seconds
        ActivityTimeline timeline = calculator.calculate(10000, config);

        assertThat(timeline.activityEnd()).isEqualTo(FIXED_NOW);
        assertThat(timeline.movingDurationSeconds()).isCloseTo(1000, within(0.01));
        assertThat(timeline.activityStart()).isEqualTo(FIXED_NOW.minusSeconds(1000));
    }

    @Test
    void startAtSelectedTime_calculatesEndForward() {
        Instant start = Instant.parse("2024-06-15T08:00:00Z");
        ActivityConfiguration config = new ActivityConfiguration(
                "Test", 36.0, 150, 1, 42L,
                new TimeConfiguration(TimeMode.START_AT_SELECTED_TIME, start), List.of());
        ActivityTimeline timeline = calculator.calculate(10000, config);

        assertThat(timeline.activityStart()).isEqualTo(start);
        assertThat(timeline.activityEnd()).isEqualTo(start.plusSeconds(1000));
    }

    @Test
    void endAtSelectedTime_calculatesStartBackward() {
        Instant end = Instant.parse("2024-06-15T12:00:00Z");
        ActivityConfiguration config = new ActivityConfiguration(
                "Test", 36.0, 150, 1, 42L,
                new TimeConfiguration(TimeMode.END_AT_SELECTED_TIME, end), List.of());
        ActivityTimeline timeline = calculator.calculate(10000, config);

        assertThat(timeline.activityEnd()).isEqualTo(end);
        assertThat(timeline.activityStart()).isEqualTo(end.minusSeconds(1000));
    }

    @Test
    void pausesAreIncludedInElapsedButNotMoving() {
        List<PauseDefinition> pauses = List.of(
                new PauseDefinition(
                        java.time.Duration.ofSeconds(200),
                        java.time.Duration.ofSeconds(300)));
        ActivityConfiguration config = new ActivityConfiguration(
                "Test", 36.0, 150, 1, 42L,
                new TimeConfiguration(TimeMode.END_NOW, null), pauses);
        ActivityTimeline timeline = calculator.calculate(10000, config);

        assertThat(timeline.movingDurationSeconds()).isCloseTo(1000, within(0.01));
        assertThat(timeline.totalPauseDurationSeconds()).isEqualTo(300);
        assertThat(timeline.elapsedDurationSeconds()).isCloseTo(1300, within(0.01));
    }
}
