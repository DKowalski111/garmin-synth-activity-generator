package com.example.syntheticfit.activity.generation;

import com.example.syntheticfit.activity.domain.PauseDefinition;
import com.example.syntheticfit.activity.domain.SportType;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RealisticSpeedProfileGenerator implements SpeedProfileGenerator {

    @Override
    public SpeedProfile generate(SpeedProfileRequest request) {
        Random rng = new Random(request.seed());
        int interval = request.recordingIntervalSeconds();
        double targetAvgSpeed = request.averageSpeedMps();
        boolean isRunning = request.sport() == SportType.RUNNING;

        int totalElapsedSamples = (int) Math.ceil(
                (request.movingDurationSeconds() + totalPauseDuration(request.pauses())) / interval);
        totalElapsedSamples = Math.max(totalElapsedSamples, 2);

        List<Double> rawSpeeds = new ArrayList<>();
        for (int i = 0; i < totalElapsedSamples; i++) {
            double progress = totalElapsedSamples > 1 ? (double) i / (totalElapsedSamples - 1) : 0;
            double base = isRunning
                    ? computeRunningSpeed(progress, targetAvgSpeed, rng)
                    : computeCyclingSpeed(progress, targetAvgSpeed, rng);
            rawSpeeds.add(base);
        }

        List<Double> speeds = applyPauses(rawSpeeds, request.pauses(), interval);

        // Normalize so integrated distance equals route distance
        double currentDist = speeds.stream().mapToDouble(s -> s * interval).sum();
        if (currentDist > 0) {
            double scale = request.routeDistanceMeters() / currentDist;
            speeds = speeds.stream().map(s -> s * scale).toList();
        }

        return new SpeedProfile(speeds, speeds.stream().mapToDouble(s -> s * interval).sum());
    }

    /** Running: very flat profile — small warm-up ramp, tight noise (±3%), no sudden drops. */
    private double computeRunningSpeed(double progress, double targetAvg, Random rng) {
        double envelope;
        if (progress < 0.05) {
            envelope = 0.85 + 0.15 * (progress / 0.05); // gentle warm-up
        } else if (progress > 0.95) {
            envelope = 0.9 + 0.1 * ((1.0 - progress) / 0.05); // gentle cool-down
        } else {
            envelope = 1.0;
        }
        double noise = 1.0 + 0.03 * rng.nextGaussian();
        return Math.max(0, targetAvg * envelope * noise);
    }

    /** Cycling: wider variation — steeper ramps, ±15% noise, occasional slow sections and stops. */
    private double computeCyclingSpeed(double progress, double targetAvg, Random rng) {
        double envelope;
        if (progress < 0.1) {
            envelope = progress / 0.1;
        } else if (progress > 0.9) {
            envelope = (1.0 - progress) / 0.1;
        } else {
            envelope = 1.0;
        }
        double noise = 1.0 + 0.15 * rng.nextGaussian();
        if (rng.nextDouble() < 0.05) noise *= 0.6; // occasional slow section
        if (rng.nextDouble() < 0.01) return 0.0;   // occasional brief stop
        return Math.max(0, targetAvg * envelope * noise);
    }

    private List<Double> applyPauses(List<Double> speeds, List<PauseDefinition> pauses, int intervalSec) {
        if (pauses == null || pauses.isEmpty()) return new ArrayList<>(speeds);
        List<Double> result = new ArrayList<>(speeds);
        for (PauseDefinition pause : pauses) {
            int startIdx = (int) (pause.offsetFromStart().getSeconds() / intervalSec);
            int endIdx = startIdx + (int) Math.ceil((double) pause.duration().getSeconds() / intervalSec);
            for (int i = startIdx; i < Math.min(endIdx, result.size()); i++) {
                result.set(i, 0.0);
            }
        }
        return result;
    }

    private double totalPauseDuration(List<PauseDefinition> pauses) {
        if (pauses == null) return 0;
        return pauses.stream().mapToLong(p -> p.duration().getSeconds()).sum();
    }
}
