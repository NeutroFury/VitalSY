package com.jonesys.vitalsy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para el endpoint POST /api/v1/auth/forgot-password.
 * Recibe únicamente el email del usuario que solicita recuperar su contraseña.
 */
@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "Formato de email inválido")
    private String email;
}
