package tn.esprit.vitalsigns.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.services.RenalFunctionService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/renal-functions")
@RequiredArgsConstructor
public class RenalFunctionController {

    private final RenalFunctionService service;

    @PostMapping
    public RenalFunction create(@RequestBody RenalFunction rf) {
        return service.create(rf);
    }

    @GetMapping
    public List<RenalFunction> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer limit) {
        if (from != null || to != null || limit != null) {
            return service.getAllFiltered(from, to, limit);
        }
        return service.getAll();
    }

    @GetMapping("/{id}")
    public RenalFunction getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public RenalFunction update(@PathVariable Long id, @RequestBody RenalFunction rf) {
        return service.update(id, rf);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/vital-sign/{vitalSignId}")
    public ResponseEntity<RenalFunction> getByVitalSignId(@PathVariable Long vitalSignId) {
        RenalFunction renalFunction = service.findByVitalSignId(vitalSignId);
        return ResponseEntity.ok(renalFunction);
    }
}
