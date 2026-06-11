package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.request.ForgotPasswordRequest;
import com.jonesys.vitalsy.dto.request.LoginRequest;
import com.jonesys.vitalsy.dto.request.RegisterRequest;
import com.jonesys.vitalsy.dto.request.ResetPasswordRequest;
import com.jonesys.vitalsy.dto.response.LoginResponse;
import com.jonesys.vitalsy.dto.response.RegisterResponse;
import com.jonesys.vitalsy.service.AuthService;
import com.jonesys.vitalsy.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Autenticación", description = "Registro, login y recuperación de contraseña")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Registrar nuevo usuario")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse resp = authService.register(request);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Login de usuario")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse resp = authService.login(request);
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /api/v1/auth/forgot-password
     *
     * <p>Endpoint público que inicia el flujo de recuperación de contraseña.
     * Siempre devuelve HTTP 200 con el mismo mensaje, independientemente de
     * si el email existe o no (prevención de user enumeration attack).
     *
     * @param request body con el campo {@code email}
     * @return HTTP 200 con mensaje informativo genérico
     */
    @Operation(summary = "Solicitar recuperación de contraseña",
               description = "Envía un correo con un enlace de recuperación válido por 15 minutos. " +
                             "La respuesta es siempre la misma para no revelar si el email está registrado.")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.iniciarRecuperacion(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "Si el correo está registrado, recibirás un enlace de recuperación en breve."
        ));
    }

    /**
     * POST /api/v1/auth/reset-password
     *
     * <p>Endpoint público para restablecer la contraseña utilizando el token.
     *
     * @param request body con el token y la nueva contraseña
     * @return HTTP 200 con mensaje de éxito
     */
    @Operation(summary = "Restablecer contraseña",
               description = "Permite cambiar la contraseña utilizando el token enviado al correo electrónico.")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetearPassword(request.getToken(), request.getNewPassword());

        return ResponseEntity.ok(Map.of(
                "message", "Contraseña actualizada exitosamente."
        ));
    }
}
