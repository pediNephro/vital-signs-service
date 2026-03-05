package tn.esprit.vitalsigns.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.vitalsigns.entities.MedicalNote;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.services.MedicalNoteService;

import java.util.List;

@RestController
@RequestMapping("/api/medical-notes")
@RequiredArgsConstructor
public class MedicalNoteController {

    @Autowired
    private final MedicalNoteService service;

    @PostMapping
    public MedicalNote create(@RequestBody MedicalNote a) {
        return service.create(a);
    }

    @GetMapping
    public List<MedicalNote> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public MedicalNote getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public MedicalNote update(@PathVariable Long id, @RequestBody MedicalNote a) {
        return service.update(id, a);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
    @GetMapping("/vital-sign/{vitalSignId}")
    public ResponseEntity<MedicalNote> getByVitalSignId(@PathVariable Long vitalSignId) {
        MedicalNote note = service.findByVitalSignId(vitalSignId);
        return ResponseEntity.ok(note);
    }
}
