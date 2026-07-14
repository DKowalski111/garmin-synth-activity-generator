package com.example.syntheticfit.api;

import com.example.syntheticfit.activity.domain.*;
import com.example.syntheticfit.routing.domain.Route;
import com.example.syntheticfit.routing.domain.RouteProcessor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class ActivityRequestMapper {

    private final Clock clock;
    private final RouteProcessor routeProcessor;

    public ActivityRequestMapper(Clock clock, RouteProcessor routeProcessor) {
        this.clock = clock;
        this.routeProcessor = routeProcessor;
    }

    public Route toRoute(ActivityApiRequest.RouteDto dto) {
        double[][] coords = dto.points().stream()
                .map(p -> new double[]{p.latitude(), p.longitude()})
                .toArray(double[][]::new);
        return routeProcessor.buildRoute(coords, "");
    }

    public ActivityConfiguration toConfig(ActivityApiRequest req) {
        TimeConfiguration timeCfg = mapTimeConfig(req.timeConfiguration(), req.fixedEndTime());
        List<PauseDefinition> pauses = mapPauses(req.pauses());
        SportType sport = parseSport(req.sport());

        double speedKmh = resolveSpeedKmh(req, sport);
        String name = req.activityName() != null ? req.activityName()
                : (sport == SportType.RUNNING ? "Synthetic Running Activity" : "Synthetic Cycling Activity");

        return new ActivityConfiguration(
                name,
                sport,
                speedKmh,
                req.averageHeartRate(),
                req.recordingIntervalSeconds(),
                req.seed(),
                timeCfg,
                pauses,
                req.cadenceSpm()
        );
    }

    private SportType parseSport(String sport) {
        if ("RUNNING".equalsIgnoreCase(sport)) return SportType.RUNNING;
        return SportType.CYCLING;
    }

    /**
     * For running, the frontend sends averagePaceMinPerKm (min/km).
     * Convert to km/h: speed = 60 / pace.
     * Fall back to averageSpeedKmh if pace is absent.
     */
    private double resolveSpeedKmh(ActivityApiRequest req, SportType sport) {
        if (sport == SportType.RUNNING && req.averagePaceMinPerKm() != null && req.averagePaceMinPerKm() > 0) {
            return 60.0 / req.averagePaceMinPerKm();
        }
        return req.averageSpeedKmh();
    }

    private TimeConfiguration mapTimeConfig(ActivityApiRequest.TimeConfigDto dto, java.time.Instant fixedEndTime) {
        TimeMode mode = TimeMode.valueOf(dto.mode());
        return switch (mode) {
            case END_NOW -> {
                if (fixedEndTime != null) {
                    yield new TimeConfiguration(TimeMode.END_AT_SELECTED_TIME, fixedEndTime);
                }
                yield new TimeConfiguration(TimeMode.END_NOW, null);
            }
            case END_AT_SELECTED_TIME -> new TimeConfiguration(TimeMode.END_AT_SELECTED_TIME, dto.selectedTime());
            case START_AT_SELECTED_TIME -> new TimeConfiguration(TimeMode.START_AT_SELECTED_TIME, dto.selectedTime());
        };
    }

    private List<PauseDefinition> mapPauses(List<ActivityApiRequest.PauseDto> dtos) {
        if (dtos == null) return List.of();
        List<PauseDefinition> result = new ArrayList<>();
        for (var dto : dtos) {
            result.add(new PauseDefinition(
                    Duration.ofSeconds(dto.offsetSeconds()),
                    Duration.ofSeconds(dto.durationSeconds())
            ));
        }
        return result;
    }
}
