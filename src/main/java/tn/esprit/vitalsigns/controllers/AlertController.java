package tn.esprit.vitalsigns.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.vitalsigns.entities.Alert;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.services.AlertService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService service;

    @PostMapping
    public Alert create(@RequestBody Alert a) {
        return service.create(a);
    }

    @GetMapping
    public List<Alert> getAll(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer limit) {
        if (patientId != null || from != null || to != null || limit != null) {
            return service.getAllFiltered(patientId, from, to, limit);
        }
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Alert getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Alert update(@PathVariable Long id, @RequestBody Alert a) {
        return service.update(id, a);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/vital-sign/{vitalSignId}")
    public ResponseEntity<Alert> getByVitalSignId(@PathVariable Long vitalSignId) {
        Alert alert = service.findByVitalSignId(vitalSignId);
        return ResponseEntity.ok(alert);
    }
}
