package tn.esprit.vitalsigns.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.vitalsigns.entities.VitalSign;
import tn.esprit.vitalsigns.services.VitalSignService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VitalSignController.class)
@DisplayName("VitalSignController - Tests d'intégration (MockMvc)")
class VitalSignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VitalSignService service;

    private ObjectMapper objectMapper;
    private VitalSign vitalSign;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

    // ─── POST /api/vital-signs ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/vital-signs - 200 OK avec le signe vital créé")
    void create_success() throws Exception {
        when(service.create(any(VitalSign.class))).thenReturn(vitalSign);

        mockMvc.perform(post("/api/vital-signs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vitalSign)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientId").value(5))
                .andExpect(jsonPath("$.temperature").value(36.8))
                .andExpect(jsonPath("$.heartRate").value(80.0));
    }

    // ─── GET /api/vital-signs (sans paramètres) ───────────────────────────────

    @Test
    @DisplayName("GET /api/vital-signs - 200 OK retourne tous les signes vitaux")
    void getAll_noParams_returnsAll() throws Exception {
        VitalSign v2 = VitalSign.builder().id(2L).patientId(6L).temperature(37.0).build();
        when(service.getAll()).thenReturn(List.of(vitalSign, v2));

        mockMvc.perform(get("/api/vital-signs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @DisplayName("GET /api/vital-signs - 200 OK liste vide")
    void getAll_empty_returns200() throws Exception {
        when(service.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/vital-signs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── GET /api/vital-signs?patientId=5 ────────────────────────────────────

    @Test
    @DisplayName("GET /api/vital-signs?patientId=5 - délègue à getAllFiltered avec patientId")
    void getAll_withPatientId_callsFiltered() throws Exception {
        when(service.getAllFiltered(eq(5L), isNull(), isNull(), isNull()))
                .thenReturn(List.of(vitalSign));

        mockMvc.perform(get("/api/vital-signs").param("patientId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].patientId").value(5));
    }

    // ─── GET /api/vital-signs?patientId=5&limit=10 ───────────────────────────

    @Test
    @DisplayName("GET /api/vital-signs?patientId=5&limit=10 - délègue avec limit")
    void getAll_withLimit_callsFiltered() throws Exception {
        when(service.getAllFiltered(eq(5L), isNull(), isNull(), eq(10)))
                .thenReturn(List.of(vitalSign));

        mockMvc.perform(get("/api/vital-signs")
                .param("patientId", "5")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ─── GET /api/vital-signs/{id} ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/vital-signs/{id} - 200 OK si signe vital trouvé")
    void getById_found_returns200() throws Exception {
        when(service.getById(1L)).thenReturn(vitalSign);

        mockMvc.perform(get("/api/vital-signs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.spo2").value(98.0))
                .andExpect(jsonPath("$.weight").value(22.5));
    }

    @Test
    @DisplayName("GET /api/vital-signs/{id} - exception si signe vital introuvable")
    void getById_notFound_throwsException() {
        when(service.getById(99L)).thenThrow(new RuntimeException("Vital sign not found"));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/vital-signs/99")))
                .hasMessageContaining("Vital sign not found");
    }

    // ─── PUT /api/vital-signs/{id} ────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/vital-signs/{id} - 200 OK avec signe vital mis à jour")
    void update_success_returns200() throws Exception {
        VitalSign updated = VitalSign.builder()
                .id(1L)
                .patientId(5L)
                .temperature(37.5)
                .heartRate(85.0)
                .systolicBP(130.0)
                .build();

        when(service.update(eq(1L), any(VitalSign.class))).thenReturn(updated);

        mockMvc.perform(put("/api/vital-signs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperature").value(37.5))
                .andExpect(jsonPath("$.heartRate").value(85.0));
    }

    // ─── DELETE /api/vital-signs/{id} ─────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/vital-signs/{id} - 200 OK après suppression")
    void delete_success_returns200() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/vital-signs/1"))
                .andExpect(status().isOk());

        verify(service).delete(1L);
    }
}
