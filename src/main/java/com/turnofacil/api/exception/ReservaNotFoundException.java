package com.turnofacil.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReservaNotFoundException extends RuntimeException {

    public ReservaNotFoundException(Long id) {
        super("Reserva no encontrada con ID: " + id);
    }

    public ReservaNotFoundException(String message) {
        super(message);
    }
}