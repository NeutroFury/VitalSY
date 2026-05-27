package com.jonesys.vitalsy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LibreCredentialsRequest(
        @NotBlank(message = "El email no puede estar vacío")
        @Email(message = "Debe ser un email válido")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía")
        String password
) {
}
