package com.turnofacil.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponse {

    private Long id;
    private String cliente;
    private String servicio;
    private LocalDateTime fechaHora;
    private String estado;
}