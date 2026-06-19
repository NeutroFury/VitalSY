package com.jonesys.vitalsy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Payload para registrar o actualizar el token FCM de un dispositivo.
 * Se recibe en PATCH /api/v1/dispositivos/fcm-token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {

    @NotBlank(message = "El fcmToken no puede estar vacío")
    private String fcmToken;

    @Pattern(regexp = "android|ios|web", message = "La plataforma debe ser 'android', 'ios' o 'web'")
    private String plataforma = "android";
}
