package tn.esprit.vitalsigns.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.esprit.vitalsigns.entities.Alert;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.repositories.AlertRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    @Autowired
    private final AlertRepository repository;

    public Alert create(Alert a) {
        return repository.save(a);
    }

    public List<Alert> getAll() {
        return repository.findAll();
    }

    /**
     * List alerts with optional time-series filters.
     *
     * @param patientId optional filter by patient
     * @param from      optional start of generation date range (inclusive)
     * @param to        optional end of generation date range (inclusive)
     * @param limit     optional max size (default 100, max 500)
     */
    public List<Alert> getAllFiltered(Long patientId, LocalDateTime from, LocalDateTime to, Integer limit) {
        int maxSize = limit != null && limit > 0 ? Math.min(limit, 500) : 100;
        PageRequest page = PageRequest.of(0, maxSize);

        if (patientId != null && from != null && to != null) {
            return repository.findByPatientIdAndGenerationDateBetweenOrderByGenerationDateDesc(
                    patientId, from, to, page);
        }
        if (patientId != null) {
            return repository.findByPatientIdOrderByGenerationDateDesc(patientId, page);
        }
        if (from != null && to != null) {
            return repository.findByGenerationDateBetweenOrderByGenerationDateDesc(from, to, page);
        }
        return repository.findAll(page).getContent();
    }

    public Alert getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Alert update(Long id, Alert a) {
        a.setId(id);
        return repository.save(a);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
    public Alert findByVitalSignId(Long vitalSignId) {
        return repository.findByVitalSign_Id(vitalSignId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Alert not found for vitalSignId: " + vitalSignId
                ));
    }
}
