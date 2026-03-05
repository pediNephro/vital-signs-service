package tn.esprit.vitalsigns.util;

import java.util.Optional;

/**
 * Pediatric renal calculations: GFR (Schwartz) and diuresis ratio (mL/kg/h).
 */
public final class RenalCalculator {

    private RenalCalculator() {}

    /** Default k for bedside Schwartz (2009) when age/sex not specified. */
    public static final double DEFAULT_SCHWARTZ_K = 0.413;

    /**
     * Estimated GFR using Schwartz formula: GFR = k * height_cm / creatinine_mg_dL.
     *
     * @param heightCm        height in cm (from vital sign)
     * @param creatinineMgDl  serum creatinine in mg/dL
     * @param coefficientK    optional k (use DEFAULT_SCHWARTZ_K if null)
     * @return GFR in mL/min/1.73m², or empty if any input invalid
     */
    public static Optional<Double> calculateGFR(Double heightCm, Double creatinineMgDl, Double coefficientK) {
        if (heightCm == null || heightCm <= 0 || creatinineMgDl == null || creatinineMgDl <= 0) {
            return Optional.empty();
        }
        double k = coefficientK != null && coefficientK > 0 ? coefficientK : DEFAULT_SCHWARTZ_K;
        double gfr = k * heightCm / creatinineMgDl;
        return Optional.of(Math.round(gfr * 10) / 10.0);
    }

    /**
     * Diuresis as urine output in mL/kg/h.
     * If periodHours is null, assumes 1 hour (i.e. urineOutput is already hourly).
     *
     * @param urineOutputMl  urine output in mL (total over period or per hour)
     * @param weightKg       weight in kg
     * @param periodHours    period over which urineOutput was collected (e.g. 24.0); if null, treated as 1h
     * @return mL/kg/h, or empty if weight or urine output invalid
     */
    public static Optional<Double> calculateDiuresisMlPerKgPerHour(
            Double urineOutputMl, Double weightKg, Double periodHours) {
        if (weightKg == null || weightKg <= 0 || urineOutputMl == null || urineOutputMl < 0) {
            return Optional.empty();
        }
        double hours = periodHours != null && periodHours > 0 ? periodHours : 1.0;
        double mlPerKgPerH = urineOutputMl / (weightKg * hours);
        return Optional.of(Math.round(mlPerKgPerH * 100) / 100.0);
    }

    /**
     * Urine output ratio: actual vs expected (e.g. 1 mL/kg/h baseline).
     * Returns actual mL/kg/h / expectedMlPerKgPerH; if expected is 0 or null, returns empty.
     */
    public static Optional<Double> calculateUrineOutputRatio(
            Double urineOutputMl, Double weightKg, Double periodHours, double expectedMlPerKgPerH) {
        return calculateDiuresisMlPerKgPerHour(urineOutputMl, weightKg, periodHours)
                .map(actual -> expectedMlPerKgPerH > 0 ? actual / expectedMlPerKgPerH : null)
                .filter(r -> r != null);
    }
}
