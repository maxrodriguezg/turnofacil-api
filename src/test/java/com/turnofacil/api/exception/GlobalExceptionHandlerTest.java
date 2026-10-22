package com.turnofacil.api.exception;

import com.turnofacil.api.dto.ErrorResponse;
import com.turnofacil.api.dto.ReservaCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request =
            new MockHttpServletRequest("POST", "/reservas");

    @Test
    void handleReservaNotFound_debeRetornar404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleReservaNotFoundException(new ReservaNotFoundException(7L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Reserva no encontrada con ID: 7");
        assertThat(response.getBody().getPath()).isEqualTo("/reservas");
    }

    @Test
    void handleBusinessException_debeRetornar400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(new BusinessException("Regla de negocio violada"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Regla de negocio violada");
    }

    @Test
    void handleConstraintViolation_debeRetornar400ConDetalleDeCampos() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<ReservaCreateRequest>> violaciones =
                validator.validate(new ReservaCreateRequest());

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolationException(
                new ConstraintViolationException(violaciones), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Validation Error");
        assertThat(response.getBody().getValidationErrors()).containsKey("cliente");
        assertThat(response.getBody().getValidationErrors()).containsKey("servicio");
    }

    @Test
    void handleGenericException_debeRetornar500SinFiltrarDetallesInternos() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(new RuntimeException("detalle interno secreto"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Ha ocurrido un error interno en el servidor");
        assertThat(response.getBody().getMessage()).doesNotContain("secreto");
    }
}