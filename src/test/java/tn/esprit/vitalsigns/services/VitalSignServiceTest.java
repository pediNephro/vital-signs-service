package tn.esprit.vitalsigns.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.vitalsigns.entities.VitalSign;
import tn.esprit.vitalsigns.repositories.VitalSignRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VitalSignService - Tests unitaires")
class VitalSignServiceTest {

    @Mock
    private VitalSignRepository repository;

    @Mock
    private AlertEvaluationService alertEvaluationService;

    @InjectMocks
    private VitalSignService service;

    private VitalSign vitalSign;

    @BeforeEach
    void setUp() {
        vitalSign = VitalSign.builder()
                .id(1L)
                .patientId(5L)
                .enteredBy(2L)
                .measurementDate(LocalDateTime.of(2026, 4, 1, 9, 0))
                .weight(22.5)
                .height(115.0)
                .systolicBP(120.0)
                .diastolicBP(75.0)
                .heartRate(80.0)
                .temperature(36.8)
                .spo2(98.0)
                .urineOutput(600.0)
                .build();
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create - persiste le signe vital et déclenche l'évaluation des alertes")
    void create_savesAndTriggersAlertEvaluation() {
        when(repository.save(any(VitalSign.class))).thenReturn(vitalSign);
        doNothing().when(alertEvaluationService)
                .evaluateAndCreateAlerts(anyLong(), any(), any());

        VitalSign result = service.create(vitalSign);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPatientId()).isEqualTo(5L);
        verify(repository).save(vitalSign);
        verify(alertEvaluationService).evaluateAndCreateAlerts(eq(5L), eq(vitalSign), any());
    }

    @Test
    @DisplayName("create - définit createdAt et updatedAt avant la sauvegarde")
    void create_setsTimestamps() {
        when(repository.save(any(VitalSign.class))).thenReturn(vitalSign);
        doNothing().when(alertEvaluationService).evaluateAndCreateAlerts(any(), any(), any());

        service.create(vitalSign);

        verify(repository).save(argThat(v ->
                v.getCreatedAt() != null && v.getUpdatedAt() != null
        ));
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll - retourne tous les signes vitaux")
    void getAll_returnsList() {
        VitalSign v2 = VitalSign.builder().id(2L).patientId(6L).build();
        when(repository.findAll()).thenReturn(List.of(vitalSign, v2));

        List<VitalSign> result = service.getAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(VitalSign::getId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("getAll - retourne liste vide si aucun signe vital")
    void getAll_empty_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<VitalSign> result = service.getAll();

        assertThat(result).isEmpty();
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById - retourne le signe vital si trouvé")
    void getById_found_returnsVitalSign() {
        when(repository.findById(1L)).thenReturn(Optional.of(vitalSign));

        VitalSign result = service.getById(1L);

        assertThat(result.getTemperature()).isEqualTo(36.8);
        assertThat(result.getHeartRate()).isEqualTo(80.0);
        assertThat(result.getSpo2()).isEqualTo(98.0);
    }

    @Test
    @DisplayName("getById - lève exception si signe vital introuvable")
    void getById_notFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Vital sign not found");
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update - met à jour le signe vital existant et déclenche les alertes")
    void update_updatesAndTriggersAlerts() {
        VitalSign incoming = VitalSign.builder()
                .temperature(37.5)
                .heartRate(90.0)
                .systolicBP(130.0)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(vitalSign));
        when(repository.save(any(VitalSign.class))).thenReturn(vitalSign);
        doNothing().when(alertEvaluationService).evaluateAndCreateAlerts(any(), any(), any());

        VitalSign result = service.update(1L, incoming);

        assertThat(result).isNotNull();
        verify(repository).save(any(VitalSign.class));
        verify(alertEvaluationService).evaluateAndCreateAlerts(any(), any(), any());
    }

    @Test
    @DisplayName("update - lève exception si signe vital introuvable")
    void update_notFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new VitalSign()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Vital sign not found");
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete - appelle deleteById avec le bon ID")
    void delete_callsRepository() {
        doNothing().when(repository).deleteById(1L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    // ─── getAllFiltered ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllFiltered(patientId) - délègue au repository avec patientId")
    void getAllFiltered_withPatientId_callsPatientQuery() {
        when(repository.findByPatientIdOrderByMeasurementDateDesc(eq(5L), any()))
                .thenReturn(List.of(vitalSign));

        List<VitalSign> result = service.getAllFiltered(5L, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientId()).isEqualTo(5L);
        verify(repository).findByPatientIdOrderByMeasurementDateDesc(eq(5L), any());
    }

    @Test
    @DisplayName("getAllFiltered(patientId, from, to) - délègue au repository avec plage de dates")
    void getAllFiltered_withDateRange_callsDateQuery() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);

        when(repository.findByPatientIdAndMeasurementDateBetweenOrderByMeasurementDateDesc(
                eq(5L), eq(from), eq(to), any()))
                .thenReturn(List.of(vitalSign));

        List<VitalSign> result = service.getAllFiltered(5L, from, to, 10);

        assertThat(result).hasSize(1);
        verify(repository).findByPatientIdAndMeasurementDateBetweenOrderByMeasurementDateDesc(
                eq(5L), eq(from), eq(to), any());
    }
}
