package tn.esprit.vitalsigns.services;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.vitalsigns.dto.PdfReportRequest;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.entities.VitalSign;
import tn.esprit.vitalsigns.repositories.RenalFunctionRepository;
import tn.esprit.vitalsigns.repositories.VitalSignRepository;

import org.springframework.data.domain.Pageable;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportService {

    // Couleurs de la charte graphique médicale
    private static final DeviceRgb COLOR_PRIMARY    = new DeviceRgb(37, 99, 235);   // bleu médical
    private static final DeviceRgb COLOR_PRIMARY_BG = new DeviceRgb(219, 234, 254); // bleu clair
    private static final DeviceRgb COLOR_DANGER     = new DeviceRgb(220, 38, 38);   // rouge alerte
    private static final DeviceRgb COLOR_WARNING    = new DeviceRgb(217, 119, 6);   // orange avertissement
    private static final DeviceRgb COLOR_SUCCESS    = new DeviceRgb(22, 163, 74);   // vert normal
    private static final DeviceRgb COLOR_GRAY_BG    = new DeviceRgb(248, 250, 252); // gris très clair
    private static final DeviceRgb COLOR_GRAY_TEXT  = new DeviceRgb(100, 116, 139); // gris texte
    private static final DeviceRgb COLOR_BORDER     = new DeviceRgb(203, 213, 225); // gris bordure

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private VitalSignRepository vitalSignRepository;

    @Autowired
    private RenalFunctionRepository renalFunctionRepository;

    /**
     * Génère le rapport PDF complet pour un patient.
     * @return tableau d'octets du PDF prêt à être envoyé en réponse HTTP.
     */
    public byte[] generateMonitoringReport(PdfReportRequest req) throws Exception {

        // Récupération des données
        List<VitalSign> vitalSigns = vitalSignRepository
                .findByPatientIdOrderByMeasurementDateDesc(req.getPatientId(), Pageable.unpaged());

        // Filtrage par dates si fourni
        if (req.getFrom() != null && req.getTo() != null) {
            LocalDate from = LocalDate.parse(req.getFrom());
            LocalDate to   = LocalDate.parse(req.getTo());
            vitalSigns = vitalSigns.stream()
                    .filter(v -> v.getMeasurementDate() != null
                            && !v.getMeasurementDate().toLocalDate().isBefore(from)
                            && !v.getMeasurementDate().toLocalDate().isAfter(to))
                    .toList();
        }

        // Récupération des fonctions rénales associées
        List<RenalFunction> renalFunctions = vitalSigns.stream()
                .filter(v -> v.getRenalFunction() != null)
                .map(VitalSign::getRenalFunction)
                .toList();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter   writer  = new PdfWriter(baos);
        PdfDocument pdf     = new PdfDocument(writer);
        Document    document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // ── EN-TÊTE ────────────────────────────────────────────────────────────
        addHeader(document, fontRegular, fontBold, req);

        // ── RÉSUMÉ STATISTIQUES ───────────────────────────────────────────────
        if (!vitalSigns.isEmpty()) {
            addSectionTitle(document, fontBold, "Résumé des constantes vitales");
            addStatsSummary(document, fontRegular, fontBold, vitalSigns);
        }

        // ── TABLEAU CONSTANTES VITALES ────────────────────────────────────────
        if (!vitalSigns.isEmpty()) {
            addSectionTitle(document, fontBold, "Historique des mesures");
            addVitalSignsTable(document, fontRegular, fontBold, vitalSigns);
        } else {
            document.add(new Paragraph("Aucune constante vitale enregistrée pour cette période.")
                    .setFont(fontRegular).setFontSize(10).setFontColor(COLOR_GRAY_TEXT));
        }

        // ── TABLEAU FONCTION RÉNALE ───────────────────────────────────────────
        if (!renalFunctions.isEmpty()) {
            addSectionTitle(document, fontBold, "Suivi de la fonction rénale (DFG)");
            addRenalFunctionTable(document, fontRegular, fontBold, renalFunctions);
        }

        // ── PIED DE PAGE ──────────────────────────────────────────────────────
        addFooter(document, fontRegular);

        document.close();
        return baos.toByteArray();
    }

    // ── SECTIONS ──────────────────────────────────────────────────────────────

    private void addHeader(Document doc, PdfFont regular, PdfFont bold, PdfReportRequest req) throws Exception {
        // Barre de titre bleue
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(COLOR_PRIMARY)
                .setBorder(Border.NO_BORDER);

        // Colonne gauche : titre
        Cell titleCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(14);
        titleCell.add(new Paragraph("Rapport de Monitoring")
                .setFont(bold).setFontSize(16).setFontColor(new DeviceRgb(255, 255, 255)));
        titleCell.add(new Paragraph("Néphrologie Pédiatrique")
                .setFont(regular).setFontSize(11).setFontColor(new DeviceRgb(186, 213, 253)));
        headerTable.addCell(titleCell);

        // Colonne droite : infos patient
        Cell infoCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(14)
                .setTextAlignment(TextAlignment.RIGHT);
        String patientLabel = req.getPatientName() != null ? req.getPatientName() : "Patient #" + req.getPatientId();
        infoCell.add(new Paragraph(patientLabel)
                .setFont(bold).setFontSize(12).setFontColor(new DeviceRgb(255, 255, 255)));
        infoCell.add(new Paragraph("ID : " + req.getPatientId())
                .setFont(regular).setFontSize(10).setFontColor(new DeviceRgb(186, 213, 253)));
        infoCell.add(new Paragraph("Généré le : " + LocalDateTime.now().format(DATETIME_FMT))
                .setFont(regular).setFontSize(9).setFontColor(new DeviceRgb(186, 213, 253)));
        if (req.getFrom() != null && req.getTo() != null) {
            infoCell.add(new Paragraph("Période : " + req.getFrom() + " → " + req.getTo())
                    .setFont(regular).setFontSize(9).setFontColor(new DeviceRgb(186, 213, 253)));
        }
        headerTable.addCell(infoCell);

        doc.add(headerTable);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void addSectionTitle(Document doc, PdfFont bold, String title) throws Exception {
        doc.add(new Paragraph("\n").setFontSize(4));
        Table bar = new Table(UnitValue.createPercentArray(new float[]{100}))
                .setWidth(UnitValue.createPercentValue(100));
        Cell cell = new Cell()
                .setBackgroundColor(COLOR_PRIMARY_BG)
                .setBorderLeft(new SolidBorder(COLOR_PRIMARY, 4))
                .setBorderRight(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER)
                .setPaddingLeft(12).setPaddingTop(8).setPaddingBottom(8);
        cell.add(new Paragraph(title).setFont(bold).setFontSize(11).setFontColor(COLOR_PRIMARY));
        bar.addCell(cell);
        doc.add(bar);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void addStatsSummary(Document doc, PdfFont regular, PdfFont bold, List<VitalSign> signs) throws Exception {
        // Calcul des stats
        double avgWeight  = signs.stream().filter(v -> v.getWeight() != null)
                .mapToDouble(VitalSign::getWeight).average().orElse(0);
        double avgSystolic = signs.stream().filter(v -> v.getSystolicBP() != null)
                .mapToDouble(VitalSign::getSystolicBP).average().orElse(0);
        double avgDiastolic = signs.stream().filter(v -> v.getDiastolicBP() != null)
                .mapToDouble(VitalSign::getDiastolicBP).average().orElse(0);
        double avgTemp    = signs.stream().filter(v -> v.getTemperature() != null)
                .mapToDouble(VitalSign::getTemperature).average().orElse(0);
        long anomalies = signs.stream().filter(this::hasAnomaly).count();

        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100));

        addStatCard(statsTable, bold, regular, "Mesures totales", String.valueOf(signs.size()), null);
        addStatCard(statsTable, bold, regular, "Poids moyen",
                avgWeight > 0 ? String.format("%.1f kg", avgWeight) : "—", null);
        addStatCard(statsTable, bold, regular, "TA moyenne",
                avgSystolic > 0 ? String.format("%.0f/%.0f mmHg", avgSystolic, avgDiastolic) : "—", null);
        addStatCard(statsTable, bold, regular, "Anomalies détectées",
                String.valueOf(anomalies), anomalies > 0 ? COLOR_DANGER : COLOR_SUCCESS);

        doc.add(statsTable);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void addStatCard(Table table, PdfFont bold, PdfFont regular,
                             String label, String value, DeviceRgb valueColor) throws Exception {
        Cell cell = new Cell()
                .setBackgroundColor(COLOR_GRAY_BG)
                .setBorder(new SolidBorder(COLOR_BORDER, 0.5f))
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER);
        cell.add(new Paragraph(label).setFont(regular).setFontSize(9).setFontColor(COLOR_GRAY_TEXT));
        DeviceRgb vColor = valueColor != null ? valueColor : COLOR_PRIMARY;
        cell.add(new Paragraph(value).setFont(bold).setFontSize(16).setFontColor(vColor));
        table.addCell(cell);
    }

    private void addVitalSignsTable(Document doc, PdfFont regular, PdfFont bold,
                                    List<VitalSign> signs) throws Exception {
        float[] cols = {18, 10, 10, 10, 18, 10, 10, 14};
        Table table = new Table(UnitValue.createPercentArray(cols))
                .setWidth(UnitValue.createPercentValue(100))
                .setFontSize(8);

        // En-tête
        String[] headers = {"Date", "Poids (kg)", "Taille (cm)", "IMC", "TA (mmHg)", "T° (°C)", "SpO2 (%)", "Diurèse (mL)"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(COLOR_PRIMARY)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(6)
                    .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(new DeviceRgb(255, 255, 255))));
        }

        // Lignes alternées
        boolean alternate = false;
        for (VitalSign v : signs) {
            DeviceRgb rowBg = alternate ? COLOR_GRAY_BG : new DeviceRgb(255, 255, 255);
            alternate = !alternate;
            boolean anomaly = hasAnomaly(v);

            addTableCell(table, v.getMeasurementDate() != null
                            ? v.getMeasurementDate().format(DATETIME_FMT) : "—",
                    regular, rowBg, anomaly ? COLOR_DANGER : null);
            addTableCell(table, formatDouble(v.getWeight()), regular, rowBg,
                    isWeightAnomaly(v.getWeight()) ? COLOR_WARNING : null);
            addTableCell(table, formatDouble(v.getHeight()), regular, rowBg, null);
            addTableCell(table, formatDouble(v.getBmi()), regular, rowBg, null);
            String ta = (v.getSystolicBP() != null && v.getDiastolicBP() != null)
                    ? v.getSystolicBP().intValue() + "/" + v.getDiastolicBP().intValue()
                    : "—";
            addTableCell(table, ta, regular, rowBg,
                    isBPAnomaly(v.getSystolicBP(), v.getDiastolicBP()) ? COLOR_DANGER : null);
            addTableCell(table, formatDouble(v.getTemperature()), regular, rowBg,
                    isTempAnomaly(v.getTemperature()) ? COLOR_WARNING : null);
            addTableCell(table, formatDouble(v.getSpo2()), regular, rowBg,
                    isSpo2Anomaly(v.getSpo2()) ? COLOR_DANGER : null);
            addTableCell(table, formatDouble(v.getUrineOutput()), regular, rowBg, null);
        }

        doc.add(table);

        // Légende des couleurs
        doc.add(new Paragraph("\n").setFontSize(2));
        doc.add(new Paragraph()
                .add(new Text("■ ").setFontColor(COLOR_DANGER).setFontSize(10))
                .add(new Text("Valeur critique   ").setFont(regular).setFontSize(8).setFontColor(COLOR_GRAY_TEXT))
                .add(new Text("■ ").setFontColor(COLOR_WARNING).setFontSize(10))
                .add(new Text("Valeur à surveiller").setFont(regular).setFontSize(8).setFontColor(COLOR_GRAY_TEXT)));
    }

    private void addRenalFunctionTable(Document doc, PdfFont regular, PdfFont bold,
                                       List<RenalFunction> renalFunctions) throws Exception {
        float[] cols = {22, 18, 18, 20, 22};
        Table table = new Table(UnitValue.createPercentArray(cols))
                .setWidth(UnitValue.createPercentValue(100))
                .setFontSize(8);

        String[] headers = {"Date calcul", "Créatinine (mg/dL)", "DFG (mL/min)", "Formule", "Stade CKD"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(COLOR_PRIMARY)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(6)
                    .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(new DeviceRgb(255, 255, 255))));
        }

        boolean alternate = false;
        for (RenalFunction rf : renalFunctions) {
            DeviceRgb rowBg = alternate ? COLOR_GRAY_BG : new DeviceRgb(255, 255, 255);
            alternate = !alternate;

            String stade = gfrToStage(rf.getGfr());
            DeviceRgb stadeColor = gfrToColor(rf.getGfr());

            addTableCell(table, rf.getCalculationDate() != null
                    ? rf.getCalculationDate().format(DATETIME_FMT) : "—", regular, rowBg, null);
            addTableCell(table, formatDouble(rf.getCreatinineLevel()), regular, rowBg, null);
            addTableCell(table, formatDouble(rf.getGfr()), regular, rowBg, stadeColor);
            addTableCell(table, rf.getGfrFormula() != null ? rf.getGfrFormula() : "—", regular, rowBg, null);
            addTableCell(table, stade, bold, rowBg, stadeColor);
        }

        doc.add(table);
    }

    private void addFooter(Document doc, PdfFont regular) throws Exception {
        doc.add(new Paragraph("\n\n").setFontSize(4));
        Table footer = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorderTop(new SolidBorder(COLOR_BORDER, 0.5f));
        footer.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingTop(8)
                .add(new Paragraph("Ce document est confidentiel et destiné exclusivement à l'usage médical.")
                        .setFont(regular).setFontSize(8).setFontColor(COLOR_GRAY_TEXT)));
        footer.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingTop(8)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("PédiNéphro — Plateforme de Suivi Pédiatrique")
                        .setFont(regular).setFontSize(8).setFontColor(COLOR_GRAY_TEXT)));
        doc.add(footer);
    }

    // ── UTILITAIRES ───────────────────────────────────────────────────────────

    private void addTableCell(Table table, String value, PdfFont font,
                              DeviceRgb bg, DeviceRgb textColor) throws Exception {
        Cell cell = new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(COLOR_BORDER, 0.3f))
                .setPadding(5);
        Paragraph p = new Paragraph(value != null ? value : "—").setFont(font).setFontSize(8);
        if (textColor != null) p.setFontColor(textColor).setBold();
        cell.add(p);
        table.addCell(cell);
    }

    private String formatDouble(Double value) {
        if (value == null) return "—";
        return value % 1 == 0 ? String.valueOf(value.intValue()) : String.format("%.1f", value);
    }

    // ── RÈGLES CLINIQUES D'ALERTE (pédiatrie standard) ─────────────────────

    private boolean hasAnomaly(VitalSign v) {
        return isBPAnomaly(v.getSystolicBP(), v.getDiastolicBP())
                || isTempAnomaly(v.getTemperature())
                || isSpo2Anomaly(v.getSpo2())
                || isWeightAnomaly(v.getWeight());
    }

    private boolean isBPAnomaly(Double sys, Double dia) {
        if (sys == null || dia == null) return false;
        return sys > 130 || sys < 70 || dia > 85 || dia < 40;
    }

    private boolean isTempAnomaly(Double temp) {
        if (temp == null) return false;
        return temp > 38.5 || temp < 35.5;
    }

    private boolean isSpo2Anomaly(Double spo2) {
        if (spo2 == null) return false;
        return spo2 < 95;
    }

    private boolean isWeightAnomaly(Double weight) {
        return false; // à enrichir avec les percentiles OMS si disponibles
    }

    private String gfrToStage(Double gfr) {
        if (gfr == null) return "—";
        if (gfr >= 90) return "G1 (Normal)";
        if (gfr >= 60) return "G2 (Légère)";
        if (gfr >= 45) return "G3a (Modérée)";
        if (gfr >= 30) return "G3b (Modérée)";
        if (gfr >= 15) return "G4 (Sévère)";
        return "G5 (Insuffisance terminale)";
    }

    private DeviceRgb gfrToColor(Double gfr) {
        if (gfr == null) return COLOR_GRAY_TEXT;
        if (gfr >= 60)   return COLOR_SUCCESS;
        if (gfr >= 30)   return COLOR_WARNING;
        return COLOR_DANGER;
    }
}