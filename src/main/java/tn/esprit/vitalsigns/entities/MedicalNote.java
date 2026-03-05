package tn.esprit.vitalsigns.entities;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "medical_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long authorId;
    private String content;
    private LocalDateTime creationDate;

    @ManyToOne
    @JoinColumn(name = "vital_sign_id")
    @JsonBackReference("vitalSign-notes")
    private VitalSign vitalSign;
}
