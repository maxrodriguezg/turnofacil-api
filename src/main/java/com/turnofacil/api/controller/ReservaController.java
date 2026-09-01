package com.turnofacil.api.controller;

import com.turnofacil.api.dto.ReservaCreateRequest;
import com.turnofacil.api.dto.ReservaResponse;
import com.turnofacil.api.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "API para gestionar reservas de atención")
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una nueva reserva", description = "Registra una nueva reserva de atención")
    public ReservaResponse crearReserva(@Valid @RequestBody ReservaCreateRequest request) {
        return reservaService.crearReserva(request);
    }

    @GetMapping
    @Operation(summary = "Listar todas las reservas", description = "Obtiene la lista de todas las reservas, opcionalmente filtradas por estado")
    public List<ReservaResponse> listarReservas(
            @Parameter(description = "Filtrar por estado (pendiente, confirmada, cancelada, etc.)")
            @RequestParam(required = false) String estado) {
        return reservaService.listarReservas(estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una reserva por ID", description = "Busca una reserva por su identificador único")
    public ReservaResponse obtenerReserva(
            @Parameter(description = "ID de la reserva", required = true)
            @PathVariable Long id) {
        return reservaService.obtenerReserva(id);
    }
}