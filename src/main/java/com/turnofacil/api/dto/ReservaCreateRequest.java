package com.turnofacil.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaCreateRequest {

    @NotBlank(message = "El cliente es obligatorio")
    @Size(min = 2, max = 80, message = "El cliente debe tener entre 2 y 80 caracteres")
    private String cliente;

    @NotBlank(message = "El servicio es obligatorio")
    @Size(min = 2, max = 80, message = "El servicio debe tener entre 2 y 80 caracteres")
    private String servicio;

    
    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La fecha y hora deben ser futuras")
    private LocalDateTime fechaHora;
}