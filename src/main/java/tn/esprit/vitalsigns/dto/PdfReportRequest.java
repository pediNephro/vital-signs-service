package tn.esprit.vitalsigns.dto;

/**
 * DTO de requête pour la génération du rapport PDF.
 * Passé en paramètre GET sur /api/vital-signs/report/pdf
 */
public class PdfReportRequest {

    private Long patientId;
    private String patientName;   // optionnel — affiché dans l'en-tête du PDF
    private String from;          // optionnel — format ISO : "2024-01-01"
    private String to;            // optionnel — format ISO : "2025-12-31"

    public PdfReportRequest() {}

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
}
