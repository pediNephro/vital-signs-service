package tn.esprit.vitalsigns.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.vitalsigns.entities.Alert;
import tn.esprit.vitalsigns.entities.MedicalNote;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    Optional<Alert> findByVitalSign_Id(Long vitalSignId);

    List<Alert> findByPatientIdOrderByGenerationDateDesc(Long patientId, Pageable pageable);

    List<Alert> findByPatientIdAndGenerationDateBetweenOrderByGenerationDateDesc(
            Long patientId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<Alert> findByGenerationDateBetweenOrderByGenerationDateDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);
}
