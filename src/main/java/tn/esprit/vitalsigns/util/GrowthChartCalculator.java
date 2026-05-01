package tn.esprit.vitalsigns.util;

import java.util.Optional;

/**
 * Growth percentile and z-score from value and age (WHO-aligned reference values).
 *
 * Uses piecewise-linear interpolation on WHO median anchor points for HEIGHT,
 * and improved polynomial approximations for WEIGHT, BMI, and HEAD_CIRCUMFERENCE.
 *
 * For production-grade accuracy, replace with full WHO LMS tables (L, M, S by age and sex).
 */
public final class GrowthChartCalculator {

    private GrowthChartCalculator() {}

    // -------------------------------------------------------------------------
    // WHO height median anchor points (boys, 0–216 months / 18 years)
    // Source: WHO Child Growth Standards & CDC reference data
    // -------------------------------------------------------------------------
    private static final double[][] HEIGHT_BREAKPOINTS = {
            {0,   49.9},
            {3,   61.4},
            {6,   67.6},
            {9,   72.0},
            {12,  75.7},
            {18,  82.3},
            {24,  87.8},
            {36,  96.1},
            {48, 103.3},
            {60, 110.0},
            {84, 122.0},
            {120, 138.5},
            {144, 152.0},
            {216, 176.5},
    };

    /**
     * Compute z-score from value and age using WHO-aligned medians.
     * Formula: z ≈ (value − M) / (S × M)  [simplified LMS with L≈1]
     *
     * @param value     measured value (weight kg, height cm, BMI, or head circumference cm)
     * @param ageMonths age in months (0–216)
     * @param chartType WEIGHT | HEIGHT | BMI | HEAD_CIRCUMFERENCE
     * @return z-score rounded to 2 decimal places, or empty if inputs are invalid
     */
    public static Optional<Double> computeZScore(Double value, Integer ageMonths, String chartType) {
        if (value == null || ageMonths == null || ageMonths < 0 || chartType == null) {
            return Optional.empty();
        }

        double m = getMedian(ageMonths, chartType);
        double s = getScale(ageMonths, chartType);

        if (m <= 0 || s <= 0) {
            return Optional.empty();
        }

        double z = (value - m) / (s * m);
        return Optional.of(Math.round(z * 100) / 100.0);
    }

    /**
     * Convert a z-score to an approximate percentile (standard normal CDF).
     *
     * @param zScore z-score value
     * @return percentile (0.0–100.0) rounded to 1 decimal place, or empty if null
     */
    public static Optional<Double> zScoreToPercentile(Double zScore) {
        if (zScore == null) {
            return Optional.empty();
        }
        double p = normCdfApprox(zScore);
        return Optional.of(Math.round(p * 1000) / 10.0);
    }

    /**
     * Compute percentile directly from value, age and chart type.
     *
     * @param value     measured value
     * @param ageMonths age in months
     * @param chartType WEIGHT | HEIGHT | BMI | HEAD_CIRCUMFERENCE
     * @return percentile (0.0–100.0) or empty if inputs are invalid
     */
    public static Optional<Double> computePercentile(Double value, Integer ageMonths, String chartType) {
        return computeZScore(value, ageMonths, chartType)
                .flatMap(GrowthChartCalculator::zScoreToPercentile);
    }

    // =========================================================================
    // MEDIAN (M) — WHO-aligned reference values per chart type
    // =========================================================================

    private static double getMedian(int ageMonths, String chartType) {
        switch (chartType.toUpperCase()) {

            case "HEIGHT":
                // Piecewise linear interpolation on WHO anchor points
                // Verified: returns exactly the 50th percentile at each anchor
                return interpolateHeight(ageMonths);

            case "WEIGHT":
                // WHO-aligned: ~3.3 kg at birth, ~9.6 at 12 mo, ~12.2 at 24 mo
                // Uses piecewise to avoid quadratic blow-up
                if (ageMonths <= 12) {
                    return 3.3 + 0.53 * ageMonths - 0.005 * ageMonths * ageMonths;
                } else if (ageMonths <= 60) {
                    return 9.6 + 0.21 * (ageMonths - 12);
                } else {
                    return 18.7 + 0.17 * (ageMonths - 60);
                }

            case "BMI":
                // Median BMI rises then dips (adiposity rebound ~4–6 years)
                if (ageMonths <= 12) {
                    return 13.0 + 0.33 * ageMonths;
                } else if (ageMonths <= 48) {
                    return 17.0 - 0.065 * (ageMonths - 12);
                } else {
                    return 14.8 + 0.02 * (ageMonths - 48);
                }

            case "HEAD_CIRCUMFERENCE":
                // WHO: ~34 at birth, ~46 at 12 mo, ~49 at 36 mo
                if (ageMonths <= 12) {
                    return 34.0 + 1.0 * ageMonths - 0.03 * ageMonths * ageMonths;
                } else {
                    return 46.0 + 0.1 * (ageMonths - 12);
                }

            default:
                return 0;
        }
    }

    /**
     * Piecewise linear interpolation over HEIGHT_BREAKPOINTS.
     * Returns median height (cm) for the given age in months.
     */
    private static double interpolateHeight(int ageMonths) {
        if (ageMonths <= HEIGHT_BREAKPOINTS[0][0]) {
            return HEIGHT_BREAKPOINTS[0][1];
        }
        if (ageMonths >= HEIGHT_BREAKPOINTS[HEIGHT_BREAKPOINTS.length - 1][0]) {
            return HEIGHT_BREAKPOINTS[HEIGHT_BREAKPOINTS.length - 1][1];
        }
        for (int i = 0; i < HEIGHT_BREAKPOINTS.length - 1; i++) {
            double a0 = HEIGHT_BREAKPOINTS[i][0];
            double m0 = HEIGHT_BREAKPOINTS[i][1];
            double a1 = HEIGHT_BREAKPOINTS[i + 1][0];
            double m1 = HEIGHT_BREAKPOINTS[i + 1][1];
            if (ageMonths >= a0 && ageMonths <= a1) {
                double t = (ageMonths - a0) / (a1 - a0);
                return m0 + t * (m1 - m0);
            }
        }
        return 0; // unreachable
    }

    // =========================================================================
    // SCALE (S) — coefficient of variation, i.e. SD / Median
    // =========================================================================

    private static double getScale(int ageMonths, String chartType) {
        switch (chartType.toUpperCase()) {
            case "HEIGHT":
                // Real height SD ≈ 2–3 cm; S = SD/M ≈ 0.04
                return 0.04;
            case "WEIGHT":
                // Weight CV increases slightly with age
                return 0.14 + 0.005 * Math.min(ageMonths / 12.0, 3);
            case "BMI":
                return 0.14;
            case "HEAD_CIRCUMFERENCE":
                return 0.03;
            default:
                return 0.10;
        }
    }

    // =========================================================================
    // Approximate standard normal CDF (Abramowitz & Stegun 26.2.17)
    // =========================================================================

    private static double normCdfApprox(double z) {
        if (z < -8) return 0.0;
        if (z > 8)  return 1.0;
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(z));
        double d = 0.3989423 * Math.exp(-z * z / 2.0);
        double p = d * t * (0.3193815
                + t * (-0.3565638
                + t * ( 1.781478
                + t * (-1.821256
                + t *   1.330274))));
        return z >= 0 ? 1.0 - p : p;
    }
}