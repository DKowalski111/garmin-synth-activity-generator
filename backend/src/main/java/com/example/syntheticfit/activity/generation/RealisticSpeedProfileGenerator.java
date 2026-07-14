package com.example.syntheticfit.activity.generation;

import com.example.syntheticfit.activity.domain.PauseDefinition;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class RealisticSpeedProfileGenerator implements SpeedProfileGenerator {

    @Override
    public SpeedProfile generate(SpeedProfileRequest request) {
        Random rng = new Random(request.seed());
        int interval = request.recordingIntervalSeconds();
        double targetAvgSpeed = request.averageSpeedMps();

        // Build a timeline of elapsed seconds → paused?
        int totalElapsedSamples = (int) Math.ceil(
                (request.movingDurationSeconds() + totalPauseDuration(request.pauses())) / interval);
        totalElapsedSamples = Math.max(totalElapsedSamples, 2);

        // Generate raw moving speeds for each sample position
        List<Double> rawSpeeds = new ArrayList<>();
        double phase = 0.0;

        for (int i = 0; i < totalElapsedSamples; i++) {
            double progress = totalElapsedSamples > 1 ? (double) i / (totalElapsedSamples - 1) : 0;
            double base = computeBaseSpeed(progress, targetAvgSpeed, rng);
            rawSpeeds.add(base);
        }

        // Determine which samples fall in pauses (speed = 0)
        List<Double> speeds = applyPauses(rawSpeeds, request.pauses(), interval);

        // Normalize so integrated distance equals route distance
        double currentDist = speeds.stream().mapToDouble(s -> s * interval).sum();
        if (currentDist > 0) {
            double scale = request.routeDistanceMeters() / currentDist;
            speeds = speeds.stream().map(s -> s * scale).toList();
        }

        double totalDist = speeds.stream().mapToDouble(s -> s * interval).sum();
        return new SpeedProfile(speeds, totalDist);
    }

    private double computeBaseSpeed(double progress, double targetAvg, Random rng) {
        // Bell-ish curve: slow start, peak in middle, slow end
        double envelope;
        if (progress < 0.1) {
            // acceleration
            envelope = progress / 0.1;
        } else if (progress > 0.9) {
            // deceleration
            envelope = (1.0 - progress) / 0.1;
        } else {
            envelope = 1.0;
        }

        double noise = 1.0 + 0.15 * (rng.nextGaussian());
        // Occasional slower section
        if (rng.nextDouble() < 0.05) noise *= 0.6;
        // Occasional brief stop
        if (rng.nextDouble() < 0.01) return 0.0;

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
