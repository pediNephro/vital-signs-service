package tn.esprit.vitalsigns.services;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.vitalsigns.entities.RenalFunction;
import tn.esprit.vitalsigns.entities.VitalSign;
import tn.esprit.vitalsigns.repositories.RenalFunctionRepository;
import tn.esprit.vitalsigns.repositories.VitalSignRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RenalFunctionService - Tests unitaires")
class RenalFunctionServiceTest {

    @Mock
    private RenalFunctionRepository repository;

    @Mock
    private VitalSignRepository vitalSignRepository;

    @Mock
    private AlertEvaluationService alertEvaluationService;

    @InjectMocks
    private RenalFunctionService service;

    private VitalSign vitalSign;
    private RenalFunction renalFunction;

    @BeforeEach
    void setUp() {
        vitalSign = VitalSign.builder()
                .id(10L)
                .patientId(5L)
                .height(115.0)
                .weight(22.5)
                .urineOutput(600.0)
                .build();

        renalFunction = RenalFunction.builder()
                .id(1L)
                .creatinineLevel(0.8)
                .gfrFormula("Schwartz")
                .coefficientK(0.413)
                .calculationDate(LocalDateTime.of(2026, 4, 1, 9, 0))
                .vitalSign(vitalSign)
                .build();
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll - retourne toutes les fonctions rénales")
    void getAll_returnsList() {
        RenalFunction rf2 = RenalFunction.builder().id(2L).creatinineLevel(1.2).build();
        when(repository.findAll()).thenReturn(List.of(renalFunction, rf2));

        List<RenalFunction> result = service.getAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RenalFunction::getId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("getAll - retourne liste vide si aucune entrée")
    void getAll_empty_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<RenalFunction> result = service.getAll();

        assertThat(result).isEmpty();
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById - retourne la fonction rénale si trouvée")
    void getById_found_returnsRenalFunction() {
        when(repository.findById(1L)).thenReturn(Optional.of(renalFunction));

        RenalFunction result = service.getById(1L);

        assertThat(result.getCreatinineLevel()).isEqualTo(0.8);
        assertThat(result.getGfrFormula()).isEqualTo("Schwartz");
    }

    @Test
    @DisplayName("getById - retourne null si introuvable")
    void getById_notFound_returnsNull() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        RenalFunction result = service.getById(99L);

        assertThat(result).isNull();
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete - appelle deleteById avec le bon ID")
    void delete_callsRepository() {
        doNothing().when(repository).deleteById(1L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update - met à jour la créatinine et relance les alertes")
    void update_updatesCreatinineAndTriggersAlerts() {
        RenalFunction incoming = RenalFunction.builder()
                .creatinineLevel(1.1)
                .gfrFormula("Schwartz")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(renalFunction));
        when(repository.save(any(RenalFunction.class))).thenReturn(renalFunction);
        doNothing().when(alertEvaluationService).evaluateAndCreateAlerts(any(), any(), any());

        RenalFunction result = service.update(1L, incoming);

        assertThat(result).isNotNull();
        verify(repository).save(any(RenalFunction.class));
        verify(alertEvaluationService).evaluateAndCreateAlerts(any(), any(), any());
    }

    @Test
    @DisplayName("update - lève EntityNotFoundException si introuvable")
    void update_notFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new RenalFunction()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("RenalFunction not found");
    }

    // ─── findByVitalSignId ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByVitalSignId - retourne la fonction rénale liée au signe vital")
    void findByVitalSignId_found_returnsRenalFunction() {
        when(repository.findByVitalSign_Id(10L)).thenReturn(Optional.of(renalFunction));

        RenalFunction result = service.findByVitalSignId(10L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCreatinineLevel()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("findByVitalSignId - lève EntityNotFoundException si introuvable")
    void findByVitalSignId_notFound_throwsException() {
        when(repository.findByVitalSign_Id(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByVitalSignId(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("vitalSignId: 99");
    }

    // ─── getAllFiltered ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllFiltered(from, to) - délègue au repository avec plage de dates")
    void getAllFiltered_withDateRange_callsDateQuery() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);

        when(repository.findByCalculationDateBetweenOrderByCalculationDateDesc(
                eq(from), eq(to), any()))
                .thenReturn(List.of(renalFunction));

        List<RenalFunction> result = service.getAllFiltered(from, to, 10);

        assertThat(result).hasSize(1);
        verify(repository).findByCalculationDateBetweenOrderByCalculationDateDesc(
                eq(from), eq(to), any());
    }

    @Test
    @DisplayName("getAllFiltered sans dates - utilise findAll avec pagination")
    void getAllFiltered_noDates_usesPaginatedFindAll() {
        org.springframework.data.domain.Page<RenalFunction> page =
                new org.springframework.data.domain.PageImpl<>(List.of(renalFunction));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        List<RenalFunction> result = service.getAllFiltered(null, null, null);

        assertThat(result).hasSize(1);
        verify(repository).findAll(any(org.springframework.data.domain.Pageable.class));
    }
}
