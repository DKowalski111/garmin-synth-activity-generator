package com.example.syntheticfit.activity.timeline;

import com.example.syntheticfit.activity.domain.*;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ActivityTimeCalculator {

    private final Clock clock;

    public ActivityTimeCalculator(Clock clock) {
        this.clock = clock;
    }

    public ActivityTimeline calculate(double routeDistanceMeters, ActivityConfiguration config) {
        double speedMps = config.averageSpeedKmh() / 3.6;
        double movingDurationSeconds = routeDistanceMeters / speedMps;

        double pauseSeconds = config.pauses().stream()
                .mapToLong(p -> p.duration().getSeconds())
                .sum();

        double elapsedSeconds = movingDurationSeconds + pauseSeconds;

        Instant start;
        Instant end;

        switch (config.timeConfiguration().mode()) {
            case END_NOW -> {
                end = clock.instant();
                start = end.minusMillis((long) (elapsedSeconds * 1000));
            }
            case END_AT_SELECTED_TIME -> {
                end = config.timeConfiguration().selectedTime();
                start = end.minusMillis((long) (elapsedSeconds * 1000));
            }
            case START_AT_SELECTED_TIME -> {
                start = config.timeConfiguration().selectedTime();
                end = start.plusMillis((long) (elapsedSeconds * 1000));
            }
            default -> throw new IllegalArgumentException("Unknown time mode: " + config.timeConfiguration().mode());
        }

        return new ActivityTimeline(start, end, movingDurationSeconds, elapsedSeconds, pauseSeconds);
    }
}
