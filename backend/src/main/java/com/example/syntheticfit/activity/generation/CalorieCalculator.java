package com.example.syntheticfit.activity.generation;

/**
 * Estimates calories burned using MET (Metabolic Equivalent of Task) values
 * for cycling at various speeds, applied per activity sample.
 *
 * Formula: kcal = MET × weight_kg × duration_hours
 *
 * MET values sourced from the Compendium of Physical Activities (Ainsworth et al.)
 */
public final class CalorieCalculator {

    // Default assumed body weight when none is provided
    static final double DEFAULT_WEIGHT_KG = 75.0;

    private CalorieCalculator() {}

    /**
     * Returns total calories for the activity given a list of per-sample speeds
     * and the recording interval in seconds.
     */
    public static int calculate(java.util.List<Double> speedsMps, int intervalSeconds) {
        double totalKcal = 0.0;
        double intervalHours = intervalSeconds / 3600.0;
        for (double speed : speedsMps) {
            double met = metForSpeed(speed);
            totalKcal += met * DEFAULT_WEIGHT_KG * intervalHours;
        }
        return (int) Math.round(totalKcal);
    }

    /**
     * Maps cycling speed (m/s) to a MET value.
     * Based on Compendium of Physical Activities cycling categories.
     */
    static double metForSpeed(double speedMps) {
        double kmh = speedMps * 3.6;
        if (speedMps == 0.0) return 1.0;      // resting / pause
        if (kmh < 16.0)      return 4.0;       // leisure, < 16 km/h
        if (kmh < 19.0)      return 6.0;       // light, 16–19 km/h
        if (kmh < 22.5)      return 8.0;       // moderate, 19–22 km/h
        if (kmh < 25.5)      return 10.0;      // vigorous, 22–25 km/h
        if (kmh < 30.0)      return 12.0;      // racing, 25–30 km/h
        return 16.0;                            // > 30 km/h
    }
}
