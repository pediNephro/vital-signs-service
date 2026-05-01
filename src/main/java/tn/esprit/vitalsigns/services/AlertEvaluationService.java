package tn.esprit.vitalsigns.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.vitalsigns.entities.Alert;
import tn.esprit.vitalsigns.entities.AlertThreshold;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.entities.VitalSign;
import tn.esprit.vitalsigns.repositories.AlertRepository;
import tn.esprit.vitalsigns.repositories.AlertThresholdRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Evaluates active alert thresholds for a patient and creates alerts when vital sign or renal values breach thresholds.
 */
@Service
@RequiredArgsConstructor
public class AlertEvaluationService {

    private final AlertThresholdRepository thresholdRepository;
    private final AlertRepository alertRepository;

    /**
     * Evaluate thresholds for the given patient and create alerts for any breaches.
     * Uses vital sign and optional renal function values.
     */
    public void evaluateAndCreateAlerts(Long patientId, VitalSign vitalSign, RenalFunction renalFunction) {
        if (patientId == null) {
            patientId = vitalSign != null ? vitalSign.getPatientId() : null;
        }
        if (patientId == null) return;

        List<AlertThreshold> thresholds = thresholdRepository.findByPatientIdAndActiveTrue(patientId);
        LocalDateTime now = LocalDateTime.now();

        for (AlertThreshold t : thresholds) {
            String param = t.getParameter();
            if (param == null) continue;

            Double value = getValueForParameter(param, vitalSign, renalFunction);
            if (value == null) continue;

            boolean breached = false;
            String message = null;
            String severity = t.getSeverity() != null ? t.getSeverity() : "WARNING";

            if (t.getMinThreshold() != null && value < t.getMinThreshold()) {
                breached = true;
                message = t.getCustomMessage() != null ? t.getCustomMessage()
                        : String.format("%s below threshold: %.2f < %.2f", param, value, t.getMinThreshold());
            }
            if (t.getMaxThreshold() != null && value > t.getMaxThreshold()) {
                breached = true;
                message = t.getCustomMessage() != null ? t.getCustomMessage()
                        : String.format("%s above threshold: %.2f > %.2f", param, value, t.getMaxThreshold());
            }

            if (breached && message != null) {
                Alert alert = Alert.builder()
                        .patientId(patientId)
                        .type(deriveAlertType(param))
                        .severity(severity)
                        .message(message)
                        .measuredValue(value)
                        .threshold(t.getMinThreshold() != null && value < t.getMinThreshold() ? t.getMinThreshold() : t.getMaxThreshold())
                        .parameter(param)
                        .acknowledged(false)
                        .generationDate(now)
                        .vitalSign(vitalSign)
                        .build();
                alertRepository.save(alert);
            }
        }
    }

    private Double getValueForParameter(String parameter, VitalSign vs, RenalFunction rf) {
        if (parameter == null) return null;
        String p = parameter.toUpperCase();

        if (vs != null) {
            switch (p) {
                case "SYSTOLIC_BP": return vs.getSystolicBP();
                case "DIASTOLIC_BP": return vs.getDiastolicBP();
                case "HEART_RATE": return vs.getHeartRate();
                case "TEMPERATURE": return vs.getTemperature();
                case "SPO2": return vs.getSpo2();
                case "URINE_OUTPUT": return vs.getUrineOutput();
                case "WEIGHT": return vs.getWeight();
                case "HEIGHT": return vs.getHeight();
                case "BMI": return vs.getBmi();
                default:
                    break;
            }
        }

        if (rf != null) {
            switch (p) {
                case "GFR": return rf.getGfr();
                case "CREATININE": return rf.getCreatinineLevel();
                case "URINE_OUTPUT_RATIO": return rf.getUrineOutputRatio();
                case "CREATININE_CLEARANCE": return rf.getCreatinineClearance();
                default:
                    break;
            }
        }

        return null;
    }

    private String deriveAlertType(String parameter) {
        if (parameter == null) return "THRESHOLD_BREACH";
        switch (parameter.toUpperCase()) {
            case "SYSTOLIC_BP":
            case "DIASTOLIC_BP": return "HYPERTENSION";
            case "GFR":
            case "CREATININE": return "CRITICAL_GFR";
            case "URINE_OUTPUT":
            case "URINE_OUTPUT_RATIO": return "OLIGURIA";
            case "SPO2": return "HYPOXIA";
            default: return "THRESHOLD_BREACH";
        }
    }
}
