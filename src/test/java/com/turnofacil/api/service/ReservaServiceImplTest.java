package com.turnofacil.api.service;

import com.turnofacil.api.dto.ReservaCreateRequest;
import com.turnofacil.api.dto.ReservaResponse;
import com.turnofacil.api.exception.ReservaNotFoundException;
import com.turnofacil.api.model.Reserva;
import com.turnofacil.api.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Reserva crearReservaEjemplo(Long id, String estado) {
        return Reserva.builder()
                .id(id)
                .cliente("Juan Pérez")
                .servicio("Corte de cabello")
                .fechaHora(LocalDateTime.now().plusDays(1))
                .estado(estado)
                .build();
    }

    @Test
    void crearReserva_debeGuardarConEstadoPendienteYRetornarRespuesta() {
        LocalDateTime fecha = LocalDateTime.now().plusDays(2);
        ReservaCreateRequest request = ReservaCreateRequest.builder()
                .cliente("Ana Soto")
                .servicio("Masaje")
                .fechaHora(fecha)
                .build();
        Reserva guardada = Reserva.builder()
                .id(1L).cliente("Ana Soto").servicio("Masaje")
                .fechaHora(fecha).estado("pendiente").build();
        when(reservaRepository.save(any(Reserva.class))).thenReturn(guardada);

        ReservaResponse response = reservaService.crearReserva(request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo("pendiente");
        assertThat(captor.getValue().getCliente()).isEqualTo("Ana Soto");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEstado()).isEqualTo("pendiente");
        assertThat(response.getFechaHora()).isEqualTo(fecha);
    }

    @Test
    void listarReservas_conEstadoNulo_debeUsarFindAll() {
        when(reservaRepository.findAll())
                .thenReturn(List.of(crearReservaEjemplo(1L, "pendiente")));

        List<ReservaResponse> resultado = reservaService.listarReservas(null);

        assertThat(resultado).hasSize(1);
        verify(reservaRepository).findAll();
        verify(reservaRepository, never()).findByEstado(any());
    }

    @Test
    void listarReservas_conEstadoEnBlanco_debeUsarFindAll() {
        when(reservaRepository.findAll()).thenReturn(List.of());

        List<ReservaResponse> resultado = reservaService.listarReservas("   ");

        assertThat(resultado).isEmpty();
        verify(reservaRepository).findAll();
        verify(reservaRepository, never()).findByEstado(any());
    }

    @Test
    void listarReservas_conEstadoConEspacios_debeRecortarYFiltrar() {
        when(reservaRepository.findByEstado("confirmada"))
                .thenReturn(List.of(crearReservaEjemplo(2L, "confirmada")));

        List<ReservaResponse> resultado = reservaService.listarReservas("  confirmada  ");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo("confirmada");
        verify(reservaRepository).findByEstado("confirmada");
        verify(reservaRepository, never()).findAll();
    }

    @Test
    void obtenerReserva_existente_debeRetornarLaReserva() {
        when(reservaRepository.findById(5L))
                .thenReturn(Optional.of(crearReservaEjemplo(5L, "pendiente")));

        ReservaResponse response = reservaService.obtenerReserva(5L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getCliente()).isEqualTo("Juan Pérez");
    }

    @Test
    void obtenerReserva_inexistente_debeLanzarReservaNotFoundException() {
        when(reservaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.obtenerReserva(999L))
                .isInstanceOf(ReservaNotFoundException.class)
                .hasMessage("Reserva no encontrada con ID: 999");
    }
}