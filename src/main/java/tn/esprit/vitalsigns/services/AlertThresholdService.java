package tn.esprit.vitalsigns.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.vitalsigns.entities.Alert;
import tn.esprit.vitalsigns.entities.AlertThreshold;
import tn.esprit.vitalsigns.repositories.AlertThresholdRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertThresholdService {

    @Autowired
    private final AlertThresholdRepository repository;

    public AlertThreshold create(AlertThreshold t) {
        return repository.save(t);
    }

    public List<AlertThreshold> getAll() {
        return repository.findAll();
    }

    public AlertThreshold getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public AlertThreshold update(Long id, AlertThreshold t) {
        t.setId(id);
        return repository.save(t);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }


}
