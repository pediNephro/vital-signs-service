package tn.esprit.vitalsigns.entities;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "alert_thresholds")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private String parameter;       // SYSTOLIC_BP, GFR, SPO2, etc.
    private Double minThreshold;
    private Double maxThreshold;
    private String severity;

    private Boolean active;
    private String customMessage;
    private Long createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
