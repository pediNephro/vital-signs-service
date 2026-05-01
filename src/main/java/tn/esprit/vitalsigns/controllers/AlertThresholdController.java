package tn.esprit.vitalsigns.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.vitalsigns.entities.AlertThreshold;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.services.AlertThresholdService;

import java.util.List;

@RestController
@RequestMapping("/api/alert-thresholds")
@RequiredArgsConstructor
public class AlertThresholdController {

    @Autowired
    private final AlertThresholdService service;

    @PostMapping
    public AlertThreshold create(@RequestBody AlertThreshold t) {
        return service.create(t);
    }

    @GetMapping
    public List<AlertThreshold> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public AlertThreshold getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public AlertThreshold update(@PathVariable Long id, @RequestBody AlertThreshold t) {
        return service.update(id, t);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

}
