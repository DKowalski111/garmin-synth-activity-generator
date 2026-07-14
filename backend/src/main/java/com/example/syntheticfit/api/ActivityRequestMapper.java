package com.example.syntheticfit.api;

import com.example.syntheticfit.activity.domain.*;
import com.example.syntheticfit.routing.domain.Route;
import com.example.syntheticfit.routing.domain.RoutePoint;
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

        return new ActivityConfiguration(
                req.activityName() != null ? req.activityName() : "Synthetic Cycling Activity",
                req.averageSpeedKmh(),
                req.averageHeartRate(),
                req.recordingIntervalSeconds(),
                req.seed(),
                timeCfg,
                pauses
        );
    }

    private TimeConfiguration mapTimeConfig(ActivityApiRequest.TimeConfigDto dto, java.time.Instant fixedEndTime) {
        TimeMode mode = TimeMode.valueOf(dto.mode());
        return switch (mode) {
            case END_NOW -> {
                if (fixedEndTime != null) {
                    // For deterministic tests/preview
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
