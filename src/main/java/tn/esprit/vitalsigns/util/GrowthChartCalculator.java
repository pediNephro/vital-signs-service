package tn.esprit.vitalsigns.util;

import java.util.Optional;

/**
 * Growth percentile and z-score from value and age (simplified WHO-style).
 * Uses approximate LMS-derived z-scores; can be replaced with full WHO tables.
 */
public final class GrowthChartCalculator {

    private GrowthChartCalculator() {}

    /**
     * Compute z-score from value and age using simplified LMS-style approximation.
     * For production, replace with WHO growth reference tables (L, M, S by age and sex).
     *
     * @param value     measured value (weight kg, height cm, BMI, or head circumference cm)
     * @param ageMonths age in months
     * @param chartType WEIGHT, HEIGHT, BMI, HEAD_CIRCUMFERENCE
     * @return z-score or empty if inputs invalid
     */
    public static Optional<Double> computeZScore(Double value, Integer ageMonths, String chartType) {
        if (value == null || ageMonths == null || ageMonths < 0 || chartType == null) {
            return Optional.empty();
        }
        // Simplified: use a rough M (median) and S (scale) by type and age for 0–24 months
        // Real implementation would use WHO tables (L, M, S) and z = ((value/M)^L - 1)/(L*S)
        double m = getApproximateMedian(ageMonths, chartType);
        double s = getApproximateScale(ageMonths, chartType);
        if (m <= 0 || s <= 0) {
            return Optional.empty();
        }
        // z ≈ (value - M) / (S * M) as simple approximation when L≈1
        double z = (value - m) / (s * m);
        return Optional.of(Math.round(z * 100) / 100.0);
    }

    /**
     * Convert z-score to approximate percentile (standard normal CDF).
     * Uses rough approximation: P(z) ≈ normCDF(z).
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
     */
    public static Optional<Double> computePercentile(Double value, Integer ageMonths, String chartType) {
        return computeZScore(value, ageMonths, chartType).flatMap(GrowthChartCalculator::zScoreToPercentile);
    }

    private static double getApproximateMedian(int ageMonths, String chartType) {
        if (chartType == null) return 0;
        switch (chartType.toUpperCase()) {
            case "WEIGHT":
                // Approximate median weight (kg) 0–24 months: ~3.5 at 0, ~10 at 12 mo, ~12 at 24 mo
                return 3.2 + 0.45 * ageMonths - 0.002 * ageMonths * ageMonths;
            case "HEIGHT":
                // Approximate median height (cm): ~50 at 0, ~75 at 12 mo, ~87 at 24 mo
                return 50 + 1.5 * ageMonths + 0.01 * ageMonths * ageMonths;
            case "BMI":
                // Median BMI ~13 at 0 mo, ~17 at 12 mo, ~16.5 at 24 mo
                return 13 + 0.35 * Math.min(ageMonths, 18) - 0.01 * ageMonths;
            case "HEAD_CIRCUMFERENCE":
                return 34 + 0.5 * ageMonths;
            default:
                return 0;
        }
    }

    private static double getApproximateScale(int ageMonths, String chartType) {
        if (chartType == null) return 0;
        switch (chartType.toUpperCase()) {
            case "WEIGHT":
                return 0.15 + 0.01 * Math.min(ageMonths / 12, 2);
            case "HEIGHT":
                return 0.03;
            case "BMI":
                return 0.15;
            case "HEAD_CIRCUMFERENCE":
                return 0.03;
            default:
                return 0.1;
        }
    }

    /** Approximate standard normal CDF (Abramowitz & Stegun). */
    private static double normCdfApprox(double z) {
        if (z < -8) return 0;
        if (z > 8) return 1;
        double t = 1 / (1 + 0.2316419 * Math.abs(z));
        double d = 0.3989423 * Math.exp(-z * z / 2);
        double p = d * t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.821256 + t * 1.330274))));
        return z >= 0 ? 1 - p : p;
    }
}
