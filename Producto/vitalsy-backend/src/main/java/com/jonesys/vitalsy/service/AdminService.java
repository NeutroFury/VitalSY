package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.dto.mapper.UsuarioMapper;
import com.jonesys.vitalsy.dto.response.UsuarioResponse;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordResetService passwordResetService;

    public AdminService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper, PasswordResetService passwordResetService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordResetService = passwordResetService;
    }

    public List<UsuarioResponse> getAllPacientes(String adminEmail) {
        log.info("🔐 Admin '{}' consultó la lista de pacientes.", adminEmail);
        return usuarioRepository.findAll()
                .stream()
                .filter(u -> !"ADMIN".equals(u.getRol()))
                .map(usuarioMapper::toResponse)
                .toList();
    }

    public List<UsuarioResponse> getAllUsuarios(String adminEmail) {
        log.info("🔐 Admin '{}' consultó la lista completa de usuarios.", adminEmail);
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    public Map<String, String> cambiarRol(Integer id, String nuevoRol, String adminEmail) {
        if (nuevoRol == null || !List.of("PACIENTE", "MEDICO", "ADMIN").contains(nuevoRol.toUpperCase())) {
            throw new RuntimeException("Rol inválido. Los valores permitidos son: PACIENTE, MEDICO, ADMIN.");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        String rolAnterior = usuario.getRol();
        usuario.setRol(nuevoRol.toUpperCase());
        usuarioRepository.save(usuario);

        log.info("🔐 Admin '{}' cambió el rol del usuario {} de {} a {}.",
                adminEmail, usuario.getEmail(), rolAnterior, nuevoRol.toUpperCase());

        return Map.of(
                "mensaje", "Rol actualizado correctamente.",
                "usuario", usuario.getEmail(),
                "rolAnterior", rolAnterior,
                "rolNuevo", usuario.getRol()
        );
    }

    public Map<String, Object> getStats(String adminEmail) {
        log.info("🔐 Admin '{}' consultó estadísticas del sistema.", adminEmail);

        List<Usuario> todos = usuarioRepository.findAll();
        long totalPacientes = todos.stream().filter(u -> "PACIENTE".equals(u.getRol())).count();
        long totalAdmins = todos.stream().filter(u -> "ADMIN".equals(u.getRol())).count();
        long totalMedicos = todos.stream().filter(u -> "MEDICO".equals(u.getRol())).count();
        long totalActivos = todos.stream().filter(u -> Boolean.TRUE.equals(u.getActivo())).count();

        return Map.of(
                "totalUsuarios", todos.size(),
                "totalPacientes", totalPacientes,
                "totalMedicos", totalMedicos,
                "totalAdmins", totalAdmins,
                "totalActivos", totalActivos
        );
    }

    public Map<String, String> toggleUserStatus(Integer id, String adminEmail) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        
        // No permitir que el admin se suspenda a sí mismo para evitar bloqueos
        if (usuario.getEmail().equals(adminEmail)) {
            throw new RuntimeException("No puedes suspender tu propia cuenta.");
        }

        usuario.setActivo(!usuario.getActivo());
        usuarioRepository.save(usuario);

        String estado = usuario.getActivo() ? "activado" : "suspendido";
        log.info("🔐 Admin '{}' ha {} la cuenta del usuario {}.", adminEmail, estado, usuario.getEmail());

        return Map.of(
                "mensaje", "Usuario " + estado + " correctamente.",
                "usuario", usuario.getEmail(),
                "activo", usuario.getActivo().toString()
        );
    }

    public Map<String, String> triggerPasswordReset(Integer id, String adminEmail) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        passwordResetService.iniciarRecuperacion(usuario.getEmail());
        
        log.info("🔐 Admin '{}' forzó envío de enlace de recuperación al usuario {}.", adminEmail, usuario.getEmail());

        return Map.of(
                "mensaje", "Correo de recuperación enviado al usuario.",
                "usuario", usuario.getEmail()
        );
    }
}
