package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio que gestiona el flujo "Olvidé mi contraseña".
 *
 * <p>Fase 1 – Generación del token y envío del correo simulado (Mailtrap).
 * Fase 2 – Validación del token y cambio de contraseña (implementar en siguiente fase).
 */
@Slf4j
@Service
public class PasswordResetService {

    private static final long TOKEN_EXPIRY_MINUTES = 15L;

    private final UsuarioRepository usuarioRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public PasswordResetService(UsuarioRepository usuarioRepository,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Inicia el flujo de recuperación de contraseña para el email dado.
     *
     * <p><strong>Seguridad:</strong> si el email NO existe en la base de datos,
     * el método retorna silenciosamente sin lanzar excepción para no revelar
     * qué cuentas están registradas (prevención de user enumeration).
     *
     * @param email dirección de correo del usuario que olvidó su contraseña
     */
    @Transactional
    public void iniciarRecuperacion(String email) {
        // Buscar usuario; si no existe, salimos sin revelar información
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            String token = generarToken();
            LocalDateTime expiracion = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

            // Persistir token y expiración
            usuario.setResetPasswordToken(token);
            usuario.setResetPasswordExpiresAt(expiracion);
            usuarioRepository.save(usuario);

            // Enviar correo con el enlace de recuperación
            try {
                enviarCorreoRecuperacion(usuario, token);
            } catch (MessagingException ex) {
                // Loguear el error pero no propagarlo al cliente para evitar
                // revelar si el email existe o no.
                log.error("Error al enviar correo de recuperación a {}: {}", email, ex.getMessage());
            }
        });

        // Respuesta idéntica tanto si el usuario existe como si no (seguridad)
        log.info("Solicitud de recuperación procesada para email: {}", email);
    }

    /**
     * Valida el token de recuperación y actualiza la contraseña si es válido.
     *
     * @param token el token UUID enviado por correo
     * @param newPassword la nueva contraseña a establecer
     */
    @Transactional
    public void resetearPassword(String token, String newPassword) {
        Usuario usuario = usuarioRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o no existe"));

        if (usuario.getResetPasswordExpiresAt() == null || LocalDateTime.now().isAfter(usuario.getResetPasswordExpiresAt())) {
            throw new IllegalArgumentException("El token ha expirado");
        }

        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        
        // Limpiar token para que no se pueda volver a usar
        usuario.setResetPasswordToken(null);
        usuario.setResetPasswordExpiresAt(null);

        usuarioRepository.save(usuario);
        log.info("Contraseña restablecida exitosamente para el usuario con ID: {}", usuario.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos privados de soporte
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Genera un token criptográficamente seguro basado en UUID v4.
     * Usa '-' como separador eliminado para obtener un string de 32 chars hex.
     */
    private String generarToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Construye y envía el correo HTML de recuperación de contraseña.
     *
     * @param usuario destinatario
     * @param token   token UUID generado
     * @throws MessagingException si el transporte SMTP falla
     */
    private void enviarCorreoRecuperacion(Usuario usuario, String token) throws MessagingException {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

        helper.setFrom(mailFrom);
        helper.setTo(usuario.getEmail());
        helper.setSubject("VitalSY – Recupera tu contraseña");
        helper.setText(construirCuerpoHtml(usuario.getNombre(), resetLink, TOKEN_EXPIRY_MINUTES), true);

        mailSender.send(mensaje);
        log.info("Correo de recuperación enviado a {} (token válido {} min)", usuario.getEmail(), TOKEN_EXPIRY_MINUTES);
    }

    /**
     * Genera el cuerpo HTML del correo de recuperación.
     *
     * @param nombre         nombre del usuario
     * @param resetLink      URL completa con el token
     * @param minutosExpiry  minutos antes de que el token expire
     * @return HTML del correo
     */
    private String construirCuerpoHtml(String nombre, String resetLink, long minutosExpiry) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <title>Recuperación de contraseña – VitalSY</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f7fb;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f7fb;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 4px 16px rgba(0,0,0,0.08);">
                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#4f46e5,#7c3aed);
                                       padding:32px 40px;text-align:center;">
                              <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:700;
                                         letter-spacing:-0.5px;">VitalSY</h1>
                              <p style="margin:6px 0 0;color:rgba(255,255,255,0.85);font-size:14px;">
                                Tu aliado en el control de la diabetes
                              </p>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:40px;">
                              <h2 style="margin:0 0 16px;color:#1e1b4b;font-size:22px;">
                                Hola, %s 👋
                              </h2>
                              <p style="color:#4b5563;font-size:15px;line-height:1.6;margin:0 0 24px;">
                                Recibimos una solicitud para restablecer la contraseña de tu cuenta VitalSY.
                                Si fuiste tú, haz clic en el botón a continuación:
                              </p>
                              <!-- CTA Button -->
                              <table cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td align="center" style="padding:8px 0 32px;">
                                    <a href="%s"
                                       style="display:inline-block;background:linear-gradient(135deg,#4f46e5,#7c3aed);
                                              color:#ffffff;font-size:16px;font-weight:600;
                                              text-decoration:none;padding:14px 36px;
                                              border-radius:8px;letter-spacing:0.3px;">
                                      Restablecer contraseña
                                    </a>
                                  </td>
                                </tr>
                              </table>
                              <p style="color:#6b7280;font-size:13px;margin:0 0 16px;">
                                ⏱ Este enlace expirará en <strong>%d minutos</strong>.
                              </p>
                              <p style="color:#6b7280;font-size:13px;margin:0 0 24px;">
                                Si no solicitaste este cambio, puedes ignorar este correo.
                                Tu contraseña actual no se verá afectada.
                              </p>
                              <!-- Fallback link -->
                              <p style="color:#9ca3af;font-size:12px;margin:0;word-break:break-all;">
                                Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
                                <a href="%s" style="color:#4f46e5;">%s</a>
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="background:#f9fafb;padding:24px 40px;text-align:center;
                                       border-top:1px solid #e5e7eb;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                © 2026 VitalSY · DuocUC Proyecto de Titulación<br>
                                Este es un correo automático, por favor no respondas a este mensaje.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nombre, resetLink, minutosExpiry, resetLink, resetLink);
    }
}
