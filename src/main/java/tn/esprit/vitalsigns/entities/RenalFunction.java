package tn.esprit.vitalsigns.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "renal_functions", indexes = @Index(name = "idx_renal_functions_calculation", columnList = "calculationDate"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RenalFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double creatinineLevel;
    private Double gfr;
    private String gfrFormula;
    private Double coefficientK;
    private Double urineOutputRatio;
    private Double creatinineClearance;
    private LocalDateTime calculationDate;

    @OneToOne
    @JoinColumn(name = "vital_sign_id", unique = true)
    @JsonBackReference("vitalSign-renalFunction")
    private VitalSign vitalSign;
}
