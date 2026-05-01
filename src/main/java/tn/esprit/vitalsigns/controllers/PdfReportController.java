package tn.esprit.vitalsigns.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.vitalsigns.dto.PdfReportRequest;
import tn.esprit.vitalsigns.services.PdfReportService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller dédié à la génération de rapports PDF.
 * Préfixe : /api/vital-signs/report
 * Accessible via Gateway : http://localhost:8082/vital-signs/api/vital-signs/report/pdf
 */
@RestController
@RequestMapping("/api/vital-signs/report")
public class PdfReportController {

    @Autowired
    private PdfReportService pdfReportService;

    /**
     * GET /api/vital-signs/report/pdf
     *
     * Paramètres :
     *   - patientId  (obligatoire) : ID du patient
     *   - patientName (optionnel)  : Nom affiché dans l'en-tête du PDF
     *   - from        (optionnel)  : Date de début, format YYYY-MM-DD
     *   - to          (optionnel)  : Date de fin,   format YYYY-MM-DD
     *
     * Exemple :
     *   GET /api/vital-signs/report/pdf?patientId=1&patientName=Ahmed+Ben+Ali&from=2024-01-01&to=2025-12-31
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generatePdf(
            @RequestParam Long patientId,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        try {
            PdfReportRequest req = new PdfReportRequest();
            req.setPatientId(patientId);
            req.setPatientName(patientName);
            req.setFrom(from);
            req.setTo(to);

            byte[] pdfBytes = pdfReportService.generateMonitoringReport(req);

            // Nom du fichier de téléchargement
            String timestamp  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String fileName   = "rapport_monitoring_patient_" + patientId + "_" + timestamp + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // "attachment" → force le téléchargement   |   "inline" → affiche dans le navigateur
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Erreur génération PDF : " + e.getMessage()).getBytes());
        }
    }
}
