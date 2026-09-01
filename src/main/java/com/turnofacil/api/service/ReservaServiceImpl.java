package com.turnofacil.api.service;

import com.turnofacil.api.dto.ReservaCreateRequest;
import com.turnofacil.api.dto.ReservaResponse;
import com.turnofacil.api.exception.ReservaNotFoundException;
import com.turnofacil.api.model.Reserva;
import com.turnofacil.api.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;

    @Override
    public ReservaResponse crearReserva(ReservaCreateRequest request) {
        Reserva reserva = Reserva.builder()
                .cliente(request.getCliente())
                .servicio(request.getServicio())
                .fechaHora(request.getFechaHora())
                .estado("pendiente")
                .build();

        Reserva savedReserva = reservaRepository.save(reserva);
        return mapToResponse(savedReserva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarReservas(String estado) {
        List<Reserva> reservas;
        String estadoFiltrado = (estado != null) ? estado.trim() : null;
        if (estadoFiltrado != null && !estadoFiltrado.isBlank()) {
            reservas = reservaRepository.findByEstado(estadoFiltrado);
        } else {
            reservas = reservaRepository.findAll();
        }
        return reservas.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse obtenerReserva(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaNotFoundException(id));
        return mapToResponse(reserva);
    }

    private ReservaResponse mapToResponse(Reserva reserva) {
        return ReservaResponse.builder()
                .id(reserva.getId())
                .cliente(reserva.getCliente())
                .servicio(reserva.getServicio())
                .fechaHora(reserva.getFechaHora())
                .estado(reserva.getEstado())
                .build();
    }

    
}