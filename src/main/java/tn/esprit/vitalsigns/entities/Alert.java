package tn.esprit.vitalsigns.entities;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_patient_generation", columnList = "patientId,generationDate"),
        @Index(name = "idx_alerts_generation", columnList = "generationDate")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;

    private String type;       // HYPERTENSION, CRITICAL_GFR, OLIGURIA, etc.
    private String severity;   // INFO, WARNING, CRITICAL
    private String message;

    private Double measuredValue;
    private Double threshold;
    private String parameter;

    private Boolean acknowledged;
    private Long acknowledgedBy;

    private LocalDateTime generationDate;
    private LocalDateTime acknowledgmentDate;
    private String acknowledgmentComment;

    @ManyToOne
    @JoinColumn(name = "vital_sign_id")
    @JsonBackReference("vitalSign-notes")
    private VitalSign vitalSign;
}

