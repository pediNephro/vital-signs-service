package tn.esprit.vitalsigns.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import tn.esprit.vitalsigns.entities.VitalSign;
import tn.esprit.vitalsigns.services.VitalSignService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vital-signs")
@RequiredArgsConstructor
public class VitalSignController {

    private final VitalSignService service;

    @PostMapping
    public VitalSign create(@RequestBody VitalSign v) {
        return service.create(v);
    }

    @GetMapping
    public List<VitalSign> getAll(
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
    public VitalSign getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public VitalSign update(@PathVariable Long id, @RequestBody VitalSign v) {
        return service.update(id, v);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

