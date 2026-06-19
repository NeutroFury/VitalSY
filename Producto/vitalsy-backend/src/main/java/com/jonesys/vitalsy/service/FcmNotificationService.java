package com.jonesys.vitalsy.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Servicio de notificaciones push mediante Firebase Cloud Messaging (FCM).
 *
 * PRINCIPIO DE FIABILIDAD CLÍNICA:
 * Toda llamada a FCM está envuelta en try/catch de manera que un fallo de red
 * o un token inválido NUNCA propaga una excepción hacia arriba. El guardado
 * de la lectura de glucosa en BD siempre tiene prioridad.
 */
@Service
@Slf4j
public class FcmNotificationService {

    @Value("${app.firebase.credentials-path}")
    private String credentialsPath;

    /**
     * Inicializa el SDK de Firebase Admin al arrancar el contexto de Spring.
     * Lee las credenciales desde la ruta configurada en application.properties.
     * Si el archivo no existe (entorno local sin Firebase), registra una
     * advertencia y continúa sin inicializar — las notificaciones simplemente
     * no se enviarán hasta que se configure.
     */
    @PostConstruct
    public void init() {
        // Evitar doble inicialización si hay múltiples contextos (tests)
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase ya estaba inicializado. Reutilizando instancia existente.");
            return;
        }

        try {
            InputStream credentialStream = resolveCredentials();
            if (credentialStream == null) {
                log.warn("⚠️  Firebase: No se encontró el archivo de credenciales en '{}'. " +
                         "Las notificaciones push estarán deshabilitadas hasta configurarlo.", credentialsPath);
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialStream))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("✅ Firebase Admin SDK inicializado correctamente.");

        } catch (IOException e) {
            log.error("❌ Error al inicializar Firebase Admin SDK: {}. " +
                      "Las notificaciones push estarán deshabilitadas.", e.getMessage());
        }
    }

    /**
     * Envía una notificación push a una lista de tokens FCM.
     * Opera en modo best-effort: un fallo NO lanza excepción.
     *
     * @param fcmTokens Lista de tokens de dispositivos activos del usuario
     * @param titulo    Título de la notificación (aparece en la barra del dispositivo)
     * @param cuerpo    Mensaje principal de la alerta
     * @param data      Datos adicionales en formato clave-valor para el frontend
     */
    public void enviarAlerta(List<String> fcmTokens, String titulo, String cuerpo, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase no está inicializado. Notificación omitida. Título: {}", titulo);
            return;
        }

        if (fcmTokens == null || fcmTokens.isEmpty()) {
            log.debug("Usuario sin tokens FCM registrados. Notificación omitida.");
            return;
        }

        for (String token : fcmTokens) {
            try {
                Message message = Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(titulo)
                                .setBody(cuerpo)
                                .build())
                        .putAllData(data)
                        // Configuración Android: alta prioridad para alertas clínicas
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setSound("default")
                                        .setChannelId("vitalsy_alertas_glucosa")
                                        .build())
                                .build())
                        // Configuración iOS: badge y sonido para alertas críticas
                        .setApnsConfig(ApnsConfig.builder()
                                .putHeader("apns-priority", "10")
                                .setAps(Aps.builder()
                                        .setSound("default")
                                        .setBadge(1)
                                        .build())
                                .build())
                        .build();

                String messageId = FirebaseMessaging.getInstance().send(message);
                log.info("📱 Notificación FCM enviada. MessageId: {}, Token: {}...", messageId, token.substring(0, Math.min(12, token.length())));

            } catch (FirebaseMessagingException e) {
                // Token inválido o expirado → loguear y continuar con el siguiente
                log.warn("⚠️  Fallo al enviar FCM al token {}...: {} ({})",
                        token.substring(0, Math.min(12, token.length())),
                        e.getMessage(),
                        e.getMessagingErrorCode());
            } catch (Exception e) {
                log.error("❌ Error inesperado al enviar notificación FCM: {}", e.getMessage());
            }
        }
    }

    /**
     * Resuelve el stream de credenciales: primero intenta el classpath,
     * luego la ruta absoluta del sistema de archivos.
     */
    private InputStream resolveCredentials() throws IOException {
        // 1. Intento classpath (útil en Docker / entornos con resources)
        InputStream stream = getClass().getClassLoader().getResourceAsStream(credentialsPath);
        if (stream != null) {
            return stream;
        }
        // 2. Intento ruta absoluta (útil en desarrollo local)
        if (Files.exists(Paths.get(credentialsPath))) {
            return Files.newInputStream(Paths.get(credentialsPath));
        }
        return null;
    }
}
