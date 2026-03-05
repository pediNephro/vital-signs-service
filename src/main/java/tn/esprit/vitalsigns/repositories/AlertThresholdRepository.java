package tn.esprit.vitalsigns.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.vitalsigns.entities.AlertThreshold;
import tn.esprit.vitalsigns.entities.MedicalNote;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, Long> {

    List<AlertThreshold> findByPatientIdAndActiveTrue(Long patientId);
}
