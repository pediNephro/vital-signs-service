package tn.esprit.vitalsigns.entities;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "growth_charts", indexes = {
        @Index(name = "idx_growth_charts_patient_type_gen", columnList = "patientId,chartType,generationDate"),
        @Index(name = "idx_growth_charts_patient_gen", columnList = "patientId,generationDate")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GrowthChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private String chartType; // WEIGHT, HEIGHT, BMI, HEAD_CIRCUMFERENCE

    private Integer ageMonths;
    private Double value;
    private Double percentile;
    private Double zScore;

    @Column(columnDefinition = "json")
    private String dataPoints;

    private Boolean abnormal;
    private Boolean chartBreak;
    private LocalDateTime generationDate;
}

