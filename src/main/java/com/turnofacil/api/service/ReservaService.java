package com.turnofacil.api.service;

import com.turnofacil.api.dto.ReservaCreateRequest;
import com.turnofacil.api.dto.ReservaResponse;
import com.turnofacil.api.exception.ReservaNotFoundException;

import java.util.List;

public interface ReservaService {

    ReservaResponse crearReserva(ReservaCreateRequest request);

    List<ReservaResponse> listarReservas(String estado);

    ReservaResponse obtenerReserva(Long id);
}