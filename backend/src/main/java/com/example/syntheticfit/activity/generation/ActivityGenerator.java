package com.example.syntheticfit.activity.generation;

import com.example.syntheticfit.activity.domain.*;
import com.example.syntheticfit.activity.timeline.ActivityTimeCalculator;
import com.example.syntheticfit.activity.validation.PauseValidator;
import com.example.syntheticfit.routing.domain.Route;
import com.example.syntheticfit.routing.domain.RouteProcessor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ActivityGenerator {

    private final ActivityTimeCalculator timeCalculator;
    private final SpeedProfileGenerator speedProfileGenerator;
    private final HeartRateProfileGenerator heartRateGenerator;
    private final RouteProcessor routeProcessor;
    private final PauseValidator pauseValidator;

    public ActivityGenerator(
            ActivityTimeCalculator timeCalculator,
            SpeedProfileGenerator speedProfileGenerator,
            HeartRateProfileGenerator heartRateGenerator,
            RouteProcessor routeProcessor,
            PauseValidator pauseValidator) {
        this.timeCalculator = timeCalculator;
        this.speedProfileGenerator = speedProfileGenerator;
        this.heartRateGenerator = heartRateGenerator;
        this.routeProcessor = routeProcessor;
        this.pauseValidator = pauseValidator;
    }

    public GeneratedActivity generate(Route route, ActivityConfiguration config) {
        double speedMps = config.averageSpeedKmh() / 3.6;
        Duration movingDuration = Duration.ofMillis((long) (route.distanceMeters() / speedMps * 1000));

        pauseValidator.validate(config.pauses(), movingDuration);

        ActivityTimeline timeline = timeCalculator.calculate(route.distanceMeters(), config);

        SpeedProfileRequest speedReq = new SpeedProfileRequest(
                route.distanceMeters(),
                speedMps,
                timeline.movingDurationSeconds(),
                config.recordingIntervalSeconds(),
                config.seed(),
                config.pauses()
        );
        SpeedProfile speedProfile = speedProfileGenerator.generate(speedReq);
        List<Double> speeds = speedProfile.speedsAtEachSample();

        List<Integer> heartRates = heartRateGenerator.generate(speeds, config.averageHeartRate(), config.seed());

        List<ActivitySample> samples = buildSamples(route, timeline, config, speeds, heartRates);
        ActivitySummary summary = computeSummary(samples, timeline, route.distanceMeters(),
                config.recordingIntervalSeconds());

        return new GeneratedActivity(summary, samples);
    }

    private List<ActivitySample> buildSamples(
            Route route, ActivityTimeline timeline,
            ActivityConfiguration config,
            List<Double> speeds, List<Integer> heartRates) {

        int interval = config.recordingIntervalSeconds();
        int n = speeds.size();
        List<ActivitySample> samples = new ArrayList<>(n);

        double accumulatedDistance = 0.0;
        double elapsedSinceStart = 0.0;

        for (int i = 0; i < n; i++) {
            double speed = speeds.get(i);
            double distDelta = speed * interval;
            accumulatedDistance = Math.min(accumulatedDistance + distDelta, route.distanceMeters());

            Instant timestamp = timeline.activityStart().plusSeconds((long) (elapsedSinceStart));

            double[] pos = routeProcessor.interpolatePosition(route, accumulatedDistance);

            boolean isPaused = speed == 0.0 && i > 0 && i < n - 1;

            int hr = i < heartRates.size() ? heartRates.get(i) : config.averageHeartRate();

            samples.add(new ActivitySample(
                    timestamp,
                    pos[0],
                    pos[1],
                    accumulatedDistance,
                    speed,
                    hr,
                    null,
                    isPaused
            ));

            elapsedSinceStart += interval;
        }

        // Force last sample to exactly match route end
        if (!samples.isEmpty()) {
            ActivitySample last = samples.get(samples.size() - 1);
            double[] endPos = routeProcessor.interpolatePosition(route, route.distanceMeters());
            samples.set(samples.size() - 1, new ActivitySample(
                    timeline.activityEnd(),
                    endPos[0],
                    endPos[1],
                    route.distanceMeters(),
                    0.0,
                    last.heartRate(),
                    last.altitudeMeters(),
                    false
            ));
        }

        return samples;
    }

    private ActivitySummary computeSummary(List<ActivitySample> samples, ActivityTimeline timeline,
                                           double routeDistance, int intervalSeconds) {
        double maxSpeed = samples.stream().mapToDouble(ActivitySample::speedMetersPerSecond).max().orElse(0);
        double avgSpeed = timeline.movingDurationSeconds() > 0
                ? routeDistance / timeline.movingDurationSeconds() : 0;

        int maxHr = samples.stream().mapToInt(ActivitySample::heartRate).max().orElse(0);
        int avgHr = (int) samples.stream().mapToInt(ActivitySample::heartRate).average().orElse(0);

        List<Double> speeds = samples.stream().map(ActivitySample::speedMetersPerSecond).toList();
        int calories = CalorieCalculator.calculate(speeds, intervalSeconds);

        return new ActivitySummary(
                timeline.activityStart(),
                timeline.activityEnd(),
                routeDistance,
                timeline.movingDurationSeconds(),
                timeline.elapsedDurationSeconds(),
                timeline.totalPauseDurationSeconds(),
                avgSpeed,
                maxSpeed,
                avgHr,
                maxHr,
                samples.size(),
                calories
        );
    }
}
