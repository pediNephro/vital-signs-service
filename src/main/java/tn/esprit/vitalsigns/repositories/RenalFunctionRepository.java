package tn.esprit.vitalsigns.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.vitalsigns.entities.RenalFunction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RenalFunctionRepository extends JpaRepository<RenalFunction, Long> {
    Optional<RenalFunction> findByVitalSign_Id(Long vitalSignId);

    List<RenalFunction> findByCalculationDateBetweenOrderByCalculationDateDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);
}