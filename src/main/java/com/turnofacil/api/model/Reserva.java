package com.turnofacil.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El cliente es obligatorio")
    @Size(min = 2, max = 80, message = "El cliente debe tener entre 2 y 80 caracteres")
    private String cliente;

    @NotBlank(message = "El servicio es obligatorio")
    @Size(min = 2, max = 80, message = "El servicio debe tener entre 2 y 80 caracteres")
    private String servicio;

    @NotNull(message = "La fecha y hora son obligatorias")
    private LocalDateTime fechaHora;

    @Builder.Default
    private String estado = "pendiente";
}