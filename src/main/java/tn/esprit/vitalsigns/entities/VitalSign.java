package tn.esprit.vitalsigns.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vital_signs", indexes = {
        @Index(name = "idx_vital_signs_patient_measurement", columnList = "patientId,measurementDate"),
        @Index(name = "idx_vital_signs_measurement", columnList = "measurementDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;   // FK → patients
    private Long enteredBy;   // FK → users
    private LocalDateTime measurementDate;

    private Double weight;
    private Double height;
    private Double headCircumference;
    private Double bmi;

    private Double systolicBP;
    private Double diastolicBP;

    private Double heartRate;
    private Double temperature;
    private Double spo2;
    private Double urineOutput;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "vitalSign", cascade = CascadeType.ALL)
    @JsonManagedReference("vitalSign-renalFunction")
    private RenalFunction renalFunction;

    @OneToMany(mappedBy = "vitalSign")
    @JsonManagedReference("vitalSign-renalFunction")
    private List<Alert> alerts;

    @OneToMany(mappedBy = "vitalSign")
    @JsonManagedReference("vitalSign-renalFunction")
    private List<MedicalNote> medicalNotes;
}

