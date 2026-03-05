package tn.esprit.vitalsigns.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.vitalsigns.entities.VitalSign;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VitalSignRepository extends JpaRepository<VitalSign, Long> {

    List<VitalSign> findByPatientIdOrderByMeasurementDateDesc(Long patientId, Pageable pageable);

    List<VitalSign> findByPatientIdAndMeasurementDateBetweenOrderByMeasurementDateDesc(
            Long patientId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<VitalSign> findByMeasurementDateBetweenOrderByMeasurementDateDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);
}

