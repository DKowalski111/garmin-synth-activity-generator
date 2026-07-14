package com.example.syntheticfit.activity.generation;

import com.example.syntheticfit.activity.domain.SportType;

/**
 * Estimates calories burned using MET (Metabolic Equivalent of Task) values
 * applied per activity sample.
 *
 * Formula: kcal = MET × weight_kg × duration_hours
 *
 * MET values sourced from the Compendium of Physical Activities (Ainsworth et al.)
 */
public final class CalorieCalculator {

    static final double DEFAULT_WEIGHT_KG = 75.0;

    private CalorieCalculator() {}

    public static int calculate(java.util.List<Double> speedsMps, int intervalSeconds, SportType sport) {
        double totalKcal = 0.0;
        double intervalHours = intervalSeconds / 3600.0;
        for (double speed : speedsMps) {
            double met = sport == SportType.RUNNING ? metForRunning(speed) : metForCycling(speed);
            totalKcal += met * DEFAULT_WEIGHT_KG * intervalHours;
        }
        return (int) Math.round(totalKcal);
    }

    /** Cycling MET by speed — Compendium of Physical Activities. */
    static double metForCycling(double speedMps) {
        double kmh = speedMps * 3.6;
        if (speedMps == 0.0) return 1.0;
        if (kmh < 16.0)      return 4.0;
        if (kmh < 19.0)      return 6.0;
        if (kmh < 22.5)      return 8.0;
        if (kmh < 25.5)      return 10.0;
        if (kmh < 30.0)      return 12.0;
        return 16.0;
    }

    /** Running MET by speed — Compendium of Physical Activities. */
    static double metForRunning(double speedMps) {
        double kmh = speedMps * 3.6;
        if (speedMps == 0.0) return 1.0;   // standing / pause
        if (kmh < 8.0)       return 6.0;   // jogging < 8 km/h
        if (kmh < 9.7)       return 8.0;   // 8–9.7 km/h
        if (kmh < 11.3)      return 9.8;   // 9.7–11.3 km/h
        if (kmh < 12.9)      return 11.0;  // 11.3–12.9 km/h
        if (kmh < 14.5)      return 11.8;  // 12.9–14.5 km/h
        if (kmh < 16.1)      return 12.8;  // 14.5–16.1 km/h
        return 14.5;                        // > 16.1 km/h (elite)
    }
}
