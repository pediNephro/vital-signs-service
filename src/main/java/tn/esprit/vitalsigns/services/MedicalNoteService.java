package tn.esprit.vitalsigns.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.vitalsigns.entities.Alert;
import tn.esprit.vitalsigns.entities.MedicalNote;
import tn.esprit.vitalsigns.repositories.MedicalNoteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalNoteService {

    @Autowired
    private final MedicalNoteRepository repository;

    public MedicalNote create(MedicalNote a) {
        return repository.save(a);
    }

    public List<MedicalNote> getAll() {
        return repository.findAll();
    }

    public MedicalNote getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public MedicalNote update(Long id, MedicalNote a) {
        a.setId(id);
        return repository.save(a);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
    public MedicalNote findByVitalSignId(Long vitalSignId) {
        return repository.findByVitalSign_Id(vitalSignId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "medicalNote not found for vitalSignId: " + vitalSignId
                ));
    }
}
