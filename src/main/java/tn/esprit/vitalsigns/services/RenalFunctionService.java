package tn.esprit.vitalsigns.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.entities.VitalSign;
import tn.esprit.vitalsigns.repositories.RenalFunctionRepository;
import tn.esprit.vitalsigns.repositories.VitalSignRepository;
import tn.esprit.vitalsigns.util.RenalCalculator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RenalFunctionService {

    @Autowired
    private final RenalFunctionRepository repository;
    @Autowired
    private final VitalSignRepository vitalSignRepository;
    @Autowired
    private final AlertEvaluationService alertEvaluationService;

    @Transactional
    public RenalFunction create(RenalFunction rf) {

        RenalFunction newRf = new RenalFunction();

        newRf.setCreatinineLevel(rf.getCreatinineLevel());
        newRf.setGfrFormula(rf.getGfrFormula());
        newRf.setCoefficientK(rf.getCoefficientK());

        newRf.setCalculationDate(
                rf.getCalculationDate() != null
                        ? rf.getCalculationDate()
                        : LocalDateTime.now()
        );

        // Attach managed VitalSign
        if (rf.getVitalSign() != null && rf.getVitalSign().getId() != null) {

            VitalSign managedVitalSign =
                    vitalSignRepository.getReferenceById(rf.getVitalSign().getId());

            newRf.setVitalSign(managedVitalSign);
        }

        enrichFromVitalSign(newRf);

        RenalFunction saved = repository.save(newRf);

        VitalSign vs = saved.getVitalSign();
        alertEvaluationService.evaluateAndCreateAlerts(
                vs != null ? vs.getPatientId() : null,
                vs,
                saved
        );

        return saved;
    }

    // =========================
    // UPDATE
    // =========================
    @Transactional
    public RenalFunction update(Long id, RenalFunction rf) {

        RenalFunction existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RenalFunction not found"));

        existing.setCreatinineLevel(rf.getCreatinineLevel());
        existing.setGfrFormula(rf.getGfrFormula());
        existing.setCoefficientK(rf.getCoefficientK());

        // Update vital sign safely
        if (rf.getVitalSign() != null && rf.getVitalSign().getId() != null) {

            VitalSign managedVitalSign =
                    vitalSignRepository.getReferenceById(rf.getVitalSign().getId());

            existing.setVitalSign(managedVitalSign);
        }

        enrichFromVitalSign(existing);

        RenalFunction saved = repository.save(existing);

        VitalSign vs = saved.getVitalSign();
        alertEvaluationService.evaluateAndCreateAlerts(
                vs != null ? vs.getPatientId() : null,
                vs,
                saved
        );

        return saved;
    }

    public List<RenalFunction> getAll() {
        return repository.findAll();
    }

    /**
     * List renal functions with optional time-series filters (by calculation date).
     */
    public List<RenalFunction> getAllFiltered(LocalDateTime from, LocalDateTime to, Integer limit) {
        int maxSize = limit != null && limit > 0 ? Math.min(limit, 500) : 100;
        PageRequest page = PageRequest.of(0, maxSize);
        if (from != null && to != null) {
            return repository.findByCalculationDateBetweenOrderByCalculationDateDesc(from, to, page);
        }
        return repository.findAll(page).getContent();
    }

    public RenalFunction getById(Long id) {
        return repository.findById(id).orElse(null);
    }



    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void enrichFromVitalSign(RenalFunction rf) {
        VitalSign vs = rf.getVitalSign();
        if (vs != null && vs.getId() != null) {
            vs = vitalSignRepository.findById(vs.getId()).orElse(vs);
            rf.setVitalSign(vs);
        }
        if (vs == null) return;

        if (vs.getHeight() != null && rf.getCreatinineLevel() != null) {
            Optional<Double> gfr = RenalCalculator.calculateGFR(
                    vs.getHeight(), rf.getCreatinineLevel(), rf.getCoefficientK());
            gfr.ifPresent(rf::setGfr);
            if (rf.getGfrFormula() == null) rf.setGfrFormula("Schwartz");
        }

        if (vs.getWeight() != null && vs.getUrineOutput() != null) {
            RenalCalculator.calculateDiuresisMlPerKgPerHour(vs.getUrineOutput(), vs.getWeight(), null)
                    .ifPresent(rf::setUrineOutputRatio);
        }
    }
    public RenalFunction findByVitalSignId(Long vitalSignId) {
        return repository.findByVitalSign_Id(vitalSignId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "RenalFunction not found for vitalSignId: " + vitalSignId
                ));
    }
}
