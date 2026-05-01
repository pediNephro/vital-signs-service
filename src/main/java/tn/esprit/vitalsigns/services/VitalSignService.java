package tn.esprit.vitalsigns.services;

import com.netflix.discovery.converters.Auto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.entities.VitalSign;
import tn.esprit.vitalsigns.repositories.VitalSignRepository;
import tn.esprit.vitalsigns.util.RenalCalculator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VitalSignService {

    @Autowired
    private final VitalSignRepository repository;
    @Autowired
    private final AlertEvaluationService alertEvaluationService;

    @Transactional
    public VitalSign create(VitalSign v) {
        LocalDateTime now = LocalDateTime.now();
        v.setCreatedAt(now);
        v.setUpdatedAt(now);
        enrichRenalFromVitalSign(v);
        VitalSign saved = repository.save(v);
        alertEvaluationService.evaluateAndCreateAlerts(saved.getPatientId(), saved, saved.getRenalFunction());
        return saved;
    }

    public List<VitalSign> getAll() {
        return repository.findAll();
    }

    /**
     * List vital signs with optional time-series filters.
     *
     * @param patientId optional filter by patient
     * @param from      optional start of measurement date range (inclusive)
     * @param to        optional end of measurement date range (inclusive)
     * @param limit     optional max size (default 100, max 500)
     */
    public List<VitalSign> getAllFiltered(Long patientId, LocalDateTime from, LocalDateTime to, Integer limit) {
        int maxSize = limit != null && limit > 0 ? Math.min(limit, 500) : 100;
        PageRequest page = PageRequest.of(0, maxSize);

        if (patientId != null && from != null && to != null) {
            return repository.findByPatientIdAndMeasurementDateBetweenOrderByMeasurementDateDesc(
                    patientId, from, to, page);
        }
        if (patientId != null) {
            return repository.findByPatientIdOrderByMeasurementDateDesc(patientId, page);
        }
        if (from != null && to != null) {
            return repository.findByMeasurementDateBetweenOrderByMeasurementDateDesc(from, to, page);
        }
        return repository.findAll(page).getContent();
    }

    public VitalSign getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vital sign not found"));
    }

    @Transactional
    public VitalSign update(Long id, VitalSign v) {
        VitalSign existing = getById(id);
        BeanUtils.copyProperties(v, existing, "id", "createdAt");
        existing.setUpdatedAt(LocalDateTime.now());
        enrichRenalFromVitalSign(existing);
        VitalSign saved = repository.save(existing);
        alertEvaluationService.evaluateAndCreateAlerts(saved.getPatientId(), saved, saved.getRenalFunction());
        return saved;
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /** Compute GFR and diuresis from vital sign and set on renal function if present. */
    private void enrichRenalFromVitalSign(VitalSign v) {
        RenalFunction rf = v.getRenalFunction();
        if (rf == null) return;

        rf.setVitalSign(v);
        rf.setCalculationDate(rf.getCalculationDate() != null ? rf.getCalculationDate() : LocalDateTime.now());

        if (v.getHeight() != null && rf.getCreatinineLevel() != null) {
            Optional<Double> gfr = RenalCalculator.calculateGFR(
                    v.getHeight(), rf.getCreatinineLevel(), rf.getCoefficientK());
            gfr.ifPresent(rf::setGfr);
            if (rf.getGfrFormula() == null) rf.setGfrFormula("Schwartz");
        }

        if (v.getWeight() != null && v.getUrineOutput() != null) {
            RenalCalculator.calculateDiuresisMlPerKgPerHour(v.getUrineOutput(), v.getWeight(), null)
                    .ifPresent(mlPerKgH -> {
                        rf.setUrineOutputRatio(mlPerKgH);
                    });
        }
    }
}
