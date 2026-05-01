package tn.esprit.vitalsigns.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.vitalsigns.entities.GrowthChart;
import tn.esprit.vitalsigns.repositories.GrowthChartRepository;
import tn.esprit.vitalsigns.util.GrowthChartCalculator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GrowthChartService {

    @Autowired
    private final GrowthChartRepository repository;

    @Transactional
    public GrowthChart create(GrowthChart g) {
        enrichPercentileAndZScore(g);
        return repository.save(g);
    }

    public List<GrowthChart> getAll() {
        return repository.findAll();
    }

    /**
     * List growth chart entries with optional time-series and type filters.
     *
     * @param patientId optional filter by patient
     * @param chartType optional filter by type (WEIGHT, HEIGHT, BMI, HEAD_CIRCUMFERENCE)
     * @param from      optional start of generation date range (inclusive)
     * @param to        optional end of generation date range (inclusive)
     * @param limit     optional max size (default 100, max 500)
     */
    public List<GrowthChart> getAllFiltered(Long patientId, String chartType,
                                            LocalDateTime from, LocalDateTime to, Integer limit) {
        int maxSize = limit != null && limit > 0 ? Math.min(limit, 500) : 100;
        PageRequest page = PageRequest.of(0, maxSize);

        if (patientId == null) {
            return repository.findAll(page).getContent();
        }
        if (chartType != null && !chartType.isBlank() && from != null && to != null) {
            return repository.findByPatientIdAndChartTypeAndGenerationDateBetweenOrderByGenerationDateDesc(
                    patientId, chartType.trim(), from, to, page);
        }
        if (chartType != null && !chartType.isBlank()) {
            return repository.findByPatientIdAndChartTypeOrderByGenerationDateDesc(patientId, chartType.trim(), page);
        }
        if (from != null && to != null) {
            return repository.findByPatientIdAndGenerationDateBetweenOrderByGenerationDateDesc(
                    patientId, from, to, page);
        }
        return repository.findByPatientIdOrderByGenerationDateDesc(patientId, page);
    }

    public GrowthChart getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional
    public GrowthChart update(Long id, GrowthChart g) {
        g.setId(id);
        enrichPercentileAndZScore(g);
        return repository.save(g);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /** Compute percentile and z-score from value and age if not already set. */
    private void enrichPercentileAndZScore(GrowthChart g) {
        if (g.getValue() == null || g.getAgeMonths() == null || g.getChartType() == null) return;

        if (g.getZScore() == null) {
            GrowthChartCalculator.computeZScore(g.getValue(), g.getAgeMonths(), g.getChartType())
                    .ifPresent(g::setZScore);
        }
        if (g.getPercentile() == null) {
            if (g.getZScore() != null) {
                GrowthChartCalculator.zScoreToPercentile(g.getZScore()).ifPresent(g::setPercentile);
            } else {
                GrowthChartCalculator.computePercentile(g.getValue(), g.getAgeMonths(), g.getChartType())
                        .ifPresent(g::setPercentile);
            }
        }
    }
}
