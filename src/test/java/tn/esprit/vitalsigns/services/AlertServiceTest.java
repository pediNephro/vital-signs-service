package tn.esprit.vitalsigns.services;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.vitalsigns.entities.Alert;
import tn.esprit.vitalsigns.repositories.AlertRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService - Tests unitaires")
class AlertServiceTest {

    @Mock
    private AlertRepository repository;

    @InjectMocks
    private AlertService service;

    private Alert alert;

    @BeforeEach
    void setUp() {
        alert = Alert.builder()
                .id(1L)
                .patientId(5L)
                .type("HYPERTENSION")
                .severity("WARNING")
                .message("Pression artérielle élevée")
                .measuredValue(145.0)
                .threshold(140.0)
                .parameter("systolicBP")
                .acknowledged(false)
                .generationDate(LocalDateTime.of(2026, 4, 1, 9, 0))
                .build();
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create - persiste et retourne l'alerte")
    void create_success() {
        when(repository.save(any(Alert.class))).thenReturn(alert);

        Alert result = service.create(alert);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("HYPERTENSION");
        assertThat(result.getSeverity()).isEqualTo("WARNING");
        verify(repository).save(alert);
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll - retourne toutes les alertes")
    void getAll_returnsList() {
        Alert a2 = Alert.builder().id(2L).patientId(6L).type("CRITICAL_GFR").severity("CRITICAL").build();
        when(repository.findAll()).thenReturn(List.of(alert, a2));

        List<Alert> result = service.getAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Alert::getType)
                .containsExactlyInAnyOrder("HYPERTENSION", "CRITICAL_GFR");
    }

    @Test
    @DisplayName("getAll - retourne liste vide si aucune alerte")
    void getAll_empty_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<Alert> result = service.getAll();

        assertThat(result).isEmpty();
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById - retourne l'alerte si trouvée")
    void getById_found_returnsAlert() {
        when(repository.findById(1L)).thenReturn(Optional.of(alert));

        Alert result = service.getById(1L);

        assertThat(result.getMessage()).isEqualTo("Pression artérielle élevée");
        assertThat(result.getMeasuredValue()).isEqualTo(145.0);
    }

    @Test
    @DisplayName("getById - retourne null si alerte introuvable")
    void getById_notFound_returnsNull() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Alert result = service.getById(99L);

        assertThat(result).isNull();
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update - met à jour l'alerte avec le bon ID")
    void update_setsIdAndSaves() {
        Alert updated = Alert.builder()
                .type("HYPERTENSION")
                .acknowledged(true)
                .acknowledgmentComment("Traité par le médecin")
                .build();

        when(repository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Alert result = service.update(1L, updated);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAcknowledged()).isTrue();
        verify(repository).save(argThat(a -> a.getId().equals(1L)));
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete - appelle deleteById avec le bon ID")
    void delete_callsRepository() {
        doNothing().when(repository).deleteById(1L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    // ─── findByVitalSignId ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByVitalSignId - retourne l'alerte liée au signe vital")
    void findByVitalSignId_found_returnsAlert() {
        when(repository.findByVitalSign_Id(10L)).thenReturn(Optional.of(alert));

        Alert result = service.findByVitalSignId(10L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("HYPERTENSION");
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
    @DisplayName("getAllFiltered(patientId) - délègue au repository avec patientId")
    void getAllFiltered_withPatientId_callsPatientQuery() {
        when(repository.findByPatientIdOrderByGenerationDateDesc(eq(5L), any()))
                .thenReturn(List.of(alert));

        List<Alert> result = service.getAllFiltered(5L, null, null, null);

        assertThat(result).hasSize(1);
        verify(repository).findByPatientIdOrderByGenerationDateDesc(eq(5L), any());
    }

    @Test
    @DisplayName("getAllFiltered(patientId, from, to) - délègue avec plage de dates")
    void getAllFiltered_withDateRange_callsDateQuery() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);

        when(repository.findByPatientIdAndGenerationDateBetweenOrderByGenerationDateDesc(
                eq(5L), eq(from), eq(to), any()))
                .thenReturn(List.of(alert));

        List<Alert> result = service.getAllFiltered(5L, from, to, 20);

        assertThat(result).hasSize(1);
        verify(repository).findByPatientIdAndGenerationDateBetweenOrderByGenerationDateDesc(
                eq(5L), eq(from), eq(to), any());
    }

    @Test
    @DisplayName("getAllFiltered - limit null utilise 100 comme valeur par défaut")
    void getAllFiltered_nullLimit_usesDefault100() {
        when(repository.findByPatientIdOrderByGenerationDateDesc(eq(5L), any()))
                .thenReturn(List.of());

        service.getAllFiltered(5L, null, null, null);

        verify(repository).findByPatientIdOrderByGenerationDateDesc(
                eq(5L), argThat(p -> p.getPageSize() == 100));
    }

    @Test
    @DisplayName("getAllFiltered - limit est plafonné à 500")
    void getAllFiltered_limitAbove500_cappedAt500() {
        when(repository.findByPatientIdOrderByGenerationDateDesc(eq(5L), any()))
                .thenReturn(List.of());

        service.getAllFiltered(5L, null, null, 1000);

        verify(repository).findByPatientIdOrderByGenerationDateDesc(
                eq(5L), argThat(p -> p.getPageSize() == 500));
    }
}
