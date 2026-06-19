package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.request.FcmTokenRequest;
import com.jonesys.vitalsy.model.DispositivoUsuario;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.DispositivoUsuarioRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controlador para gestionar el registro de dispositivos FCM.
 * Endpoint: PATCH /api/v1/dispositivos/fcm-token
 *
 * Realiza un upsert: si el token ya existe lo reactiva y actualiza la
 * plataforma; si no existe, crea un nuevo registro.
 */
@RestController
@RequestMapping("/api/v1/dispositivos")
@Slf4j
public class DispositivoController {

    private final DispositivoUsuarioRepository dispositivoRepository;
    private final UsuarioRepository usuarioRepository;

    public DispositivoController(DispositivoUsuarioRepository dispositivoRepository,
                                 UsuarioRepository usuarioRepository) {
        this.dispositivoRepository = dispositivoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra o actualiza el token FCM del dispositivo actual del usuario autenticado.
     * El token FCM NUNCA se devuelve en la respuesta por seguridad.
     */
    @PatchMapping("/fcm-token")
    public ResponseEntity<Void> registrarFcmToken(
            @Valid @RequestBody FcmTokenRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("Solicitud de registro FCM token para usuario: {} | plataforma: {}", email, request.getPlataforma());

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));

        // Upsert: buscar token existente o crear uno nuevo
        Optional<DispositivoUsuario> existente = dispositivoRepository.findByFcmToken(request.getFcmToken());

        if (existente.isPresent()) {
            DispositivoUsuario dispositivo = existente.get();
            dispositivo.setActivo(true);
            dispositivo.setPlataforma(request.getPlataforma());
            dispositivo.setUsuario(usuario); // reasignar si cambió de usuario (re-login)
            dispositivoRepository.save(dispositivo);
            log.debug("Token FCM actualizado para usuario: {}", email);
        } else {
            DispositivoUsuario nuevo = new DispositivoUsuario();
            nuevo.setUsuario(usuario);
            nuevo.setFcmToken(request.getFcmToken());
            nuevo.setPlataforma(request.getPlataforma());
            nuevo.setActivo(true);
            dispositivoRepository.save(nuevo);
            log.debug("Nuevo token FCM registrado para usuario: {}", email);
        }

        return ResponseEntity.noContent().build(); // 204 No Content
    }

    /**
     * Desactiva el token FCM del dispositivo actual (usado al hacer logout).
     */
    @DeleteMapping("/fcm-token")
    public ResponseEntity<Void> desactivarFcmToken(
            @Valid @RequestBody FcmTokenRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("Desactivando token FCM para usuario: {}", email);

        dispositivoRepository.findByFcmToken(request.getFcmToken()).ifPresent(dispositivo -> {
            dispositivo.setActivo(false);
            dispositivoRepository.save(dispositivo);
            log.debug("Token FCM desactivado para usuario: {}", email);
        });

        return ResponseEntity.noContent().build();
    }
}
