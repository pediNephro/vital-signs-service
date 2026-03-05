package tn.esprit.vitalsigns.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.vitalsigns.entities.GrowthChart;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GrowthChartRepository extends JpaRepository<GrowthChart, Long> {

    List<GrowthChart> findByPatientIdOrderByGenerationDateDesc(Long patientId, Pageable pageable);

    List<GrowthChart> findByPatientIdAndChartTypeOrderByGenerationDateDesc(Long patientId, String chartType, Pageable pageable);

    List<GrowthChart> findByPatientIdAndGenerationDateBetweenOrderByGenerationDateDesc(
            Long patientId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<GrowthChart> findByPatientIdAndChartTypeAndGenerationDateBetweenOrderByGenerationDateDesc(
            Long patientId, String chartType, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
