package tn.esprit.vitalsigns.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.vitalsigns.entities.MedicalNote;
import tn.esprit.vitalsigns.entities.RenalFunction;

import java.util.Optional;

@Repository
public interface MedicalNoteRepository extends JpaRepository<MedicalNote, Long> {
    Optional<MedicalNote> findByVitalSign_Id(Long vitalSignId);

}
