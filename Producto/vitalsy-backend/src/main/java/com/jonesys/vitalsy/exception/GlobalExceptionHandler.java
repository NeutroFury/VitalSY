package com.jonesys.vitalsy.exception;

import com.jonesys.vitalsy.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Errores de infraestructura / IA externa → 503 Service Unavailable */
    private static final java.util.Set<String> IA_ERROR_CODES = java.util.Set.of(
            "PREDICTIVE_IA_SERVER_UNAVAILABLE",
            "IA_SERVER_UNAVAILABLE",
            "CHAT_IA_SERVER_UNAVAILABLE",
            "PREDICTIVE_PAYLOAD_BUILD_ERROR"
    );

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();

        // Errores de dependencia externa (Gemini / IA): 503
        if (message != null && IA_ERROR_CODES.stream().anyMatch(message::startsWith)) {
            log.error("Error de servicio IA externo: {}", message);
            ErrorResponse errorResponse = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                    message
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
        }

        // Errores de lógica de negocio: 400
        log.error("Excepción de negocio (RuntimeException): {}", message);
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Excepción inesperada en el servidor", ex);
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ha ocurrido un error interno en el servidor. Por favor, intente más tarde."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
