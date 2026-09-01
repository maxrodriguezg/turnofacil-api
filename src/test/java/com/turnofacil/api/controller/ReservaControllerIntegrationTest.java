package com.turnofacil.api.controller;

import com.turnofacil.api.dto.ReservaCreateRequest;
import com.turnofacil.api.dto.ReservaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthCheck_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("turnofacil-api"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void healthLiveness_shouldReturnAlive() throws Exception {
        mockMvc.perform(get("/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("alive"));
    }

    @Test
    void healthReadiness_shouldReturnReady() throws Exception {
        mockMvc.perform(get("/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"));
    }

    @Test
    void crearReserva_shouldCreateAndReturnReserva() throws Exception {
        ReservaCreateRequest request = ReservaCreateRequest.builder()
                .cliente("Juan Pérez")
                .servicio("Corte de cabello")
                .fechaHora(LocalDateTime.now().plusDays(1))
                .build();

        MvcResult result = mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cliente").value("Juan Pérez"))
                .andExpect(jsonPath("$.servicio").value("Corte de cabello"))
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ReservaResponse response = objectMapper.readValue(responseBody, ReservaResponse.class);

        mockMvc.perform(get("/reservas/{id}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId()))
                .andExpect(jsonPath("$.cliente").value("Juan Pérez"));
    }

    @Test
    void crearReserva_shouldReturnBadRequestWhenClienteIsEmpty() throws Exception {
        ReservaCreateRequest request = ReservaCreateRequest.builder()
                .cliente("")
                .servicio("Corte de cabello")
                .fechaHora(LocalDateTime.now().plusDays(1))
                .build();

        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.validationErrors.cliente").exists());
    }

    @Test
    void listarReservas_shouldHandleNullAndWhitespaceEstado() throws Exception {
        mockMvc.perform(get("/reservas").param("estado", "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        
        mockMvc.perform(get("/reservas").param("estado", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void obtenerReserva_shouldReturnNotFoundWhenNotExists() throws Exception {
        mockMvc.perform(get("/reservas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Reserva no encontrada con ID: 999"));
    }

    @Test
    void crearReserva_shouldReturnBadRequestWhenFechaHoraIsPast() throws Exception {
        ReservaCreateRequest request = ReservaCreateRequest.builder()
                .cliente("Juan Luis Pérez")
                .servicio("Corte de cabello")
                .fechaHora(LocalDateTime.now().minusDays(1))
                .build();

        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.validationErrors.fechaHora").exists());
    }
}