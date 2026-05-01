package tn.esprit.vitalsigns.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.vitalsigns.entities.GrowthChart;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.services.GrowthChartService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/growth-charts")
@RequiredArgsConstructor
public class GrowthChartController {

    private final GrowthChartService service;

    @PostMapping
    public GrowthChart create(@RequestBody GrowthChart g) {
        return service.create(g);
    }

    @GetMapping
    public List<GrowthChart> getAll(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String chartType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer limit) {
        if (patientId != null || chartType != null || from != null || to != null || limit != null) {
            return service.getAllFiltered(patientId, chartType, from, to, limit);
        }
        return service.getAll();
    }

    @GetMapping("/{id}")
    public GrowthChart getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public GrowthChart update(@PathVariable Long id, @RequestBody GrowthChart g) {
        return service.update(id, g);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

}
