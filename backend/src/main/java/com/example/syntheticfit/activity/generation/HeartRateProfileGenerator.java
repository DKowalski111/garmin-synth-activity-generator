package com.example.syntheticfit.activity.generation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class HeartRateProfileGenerator {

    public List<Integer> generate(List<Double> speedsMps, int targetAvgBpm, long seed) {
        Random rng = new Random(seed + 1); // different seed from speed
        int n = speedsMps.size();
        List<Double> rawHr = new ArrayList<>(n);

        double maxSpeed = speedsMps.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxSpeed == 0) maxSpeed = 1.0;

        double restingHr = targetAvgBpm * 0.65;
        double peakHr = Math.min(220, targetAvgBpm * 1.25);

        double currentHr = restingHr;

        for (int i = 0; i < n; i++) {
            double speed = speedsMps.get(i);
            double progress = n > 1 ? (double) i / (n - 1) : 0;

            // Target HR based on speed and cardiac drift
            double intensity = maxSpeed > 0 ? speed / maxSpeed : 0;
            double cardiacDrift = 1.0 + 0.03 * progress; // mild drift
            double targetHr;

            if (speed == 0) {
                // During pause, HR drops toward resting
                targetHr = restingHr + (currentHr - restingHr) * 0.7;
            } else {
                targetHr = restingHr + (peakHr - restingHr) * intensity * cardiacDrift;
            }

            // Delayed response (low-pass filter)
            double lag = speed == 0 ? 0.15 : 0.08;
            currentHr = currentHr + lag * (targetHr - currentHr);

            // Small natural variation
            double noise = 1.0 + 0.02 * rng.nextGaussian();
            rawHr.add(Math.max(restingHr, currentHr * noise));
        }

        // Normalize so average matches target
        double currentAvg = rawHr.stream().filter(hr -> hr > restingHr * 0.8)
                .mapToDouble(Double::doubleValue).average().orElse(targetAvgBpm);
        double scale = currentAvg > 0 ? (double) targetAvgBpm / currentAvg : 1.0;

        List<Integer> result = new ArrayList<>(n);
        for (double hr : rawHr) {
            int scaled = (int) Math.round(Math.min(220, Math.max(40, hr * scale)));
            result.add(scaled);
        }
        return result;
    }
}
