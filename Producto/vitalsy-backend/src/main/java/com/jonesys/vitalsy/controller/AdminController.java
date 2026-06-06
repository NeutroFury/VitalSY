package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.response.UsuarioResponse;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UsuarioRepository usuarioRepository;

    public AdminController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * GET /api/v1/admin/usuarios
     * Devuelve la lista de todos los pacientes registrados.
     * Requiere rol ADMIN.
     */
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponse>> getAllPacientes(Authentication authentication) {
        log.info("🔐 Admin '{}' consultó la lista de pacientes.", authentication.getName());

        List<UsuarioResponse> pacientes = usuarioRepository.findAll()
                .stream()
                .filter(u -> !"ADMIN".equals(u.getRol())) // Excluye otros admins de la lista
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(pacientes);
    }

    /**
     * GET /api/v1/admin/usuarios/todos
     * Devuelve todos los usuarios incluyendo admins (para auditoría).
     * Requiere rol ADMIN.
     */
    @GetMapping("/usuarios/todos")
    public ResponseEntity<List<UsuarioResponse>> getAllUsuarios(Authentication authentication) {
        log.info("🔐 Admin '{}' consultó la lista completa de usuarios.", authentication.getName());

        List<UsuarioResponse> todos = usuarioRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(todos);
    }

    /**
     * PUT /api/v1/admin/usuarios/{id}/rol
     * Cambia el rol de un usuario. Body: { "rol": "ADMIN" | "PACIENTE" | "MEDICO" }
     * Requiere rol ADMIN.
     */
    @PutMapping("/usuarios/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Integer id,
                                         @RequestBody Map<String, String> body,
                                         Authentication authentication) {
        String nuevoRol = body.get("rol");
        if (nuevoRol == null || !List.of("PACIENTE", "MEDICO", "ADMIN").contains(nuevoRol.toUpperCase())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensaje", "Rol inválido. Los valores permitidos son: PACIENTE, MEDICO, ADMIN."
            ));
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        String rolAnterior = usuario.getRol();
        usuario.setRol(nuevoRol.toUpperCase());
        usuarioRepository.save(usuario);

        log.info("🔐 Admin '{}' cambió el rol del usuario {} de {} a {}.",
                authentication.getName(), usuario.getEmail(), rolAnterior, nuevoRol.toUpperCase());

        return ResponseEntity.ok(Map.of(
                "mensaje", "Rol actualizado correctamente.",
                "usuario", usuario.getEmail(),
                "rolAnterior", rolAnterior,
                "rolNuevo", usuario.getRol()
        ));
    }

    /**
     * GET /api/v1/admin/stats
     * Estadísticas básicas del sistema.
     * Requiere rol ADMIN.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        log.info("🔐 Admin '{}' consultó estadísticas del sistema.", authentication.getName());

        List<Usuario> todos = usuarioRepository.findAll();
        long totalPacientes = todos.stream().filter(u -> "PACIENTE".equals(u.getRol())).count();
        long totalAdmins = todos.stream().filter(u -> "ADMIN".equals(u.getRol())).count();
        long totalMedicos = todos.stream().filter(u -> "MEDICO".equals(u.getRol())).count();
        long totalActivos = todos.stream().filter(u -> Boolean.TRUE.equals(u.getActivo())).count();

        return ResponseEntity.ok(Map.of(
                "totalUsuarios", todos.size(),
                "totalPacientes", totalPacientes,
                "totalMedicos", totalMedicos,
                "totalAdmins", totalAdmins,
                "totalActivos", totalActivos
        ));
    }

    // ── Mapper privado ──────────────────────────────────────────────────────

    private UsuarioResponse mapToResponse(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .email(u.getEmail())
                .pesoActual(u.getPesoActual())
                .altura(u.getAltura())
                .insulinaLenta(u.getInsulinaLenta())
                .insulinaRapida(u.getInsulinaRapida())
                .ratioIc(u.getRatioIc())
                .factorIs(u.getFactorIs())
                .alertasGlucosa(u.getAlertasGlucosa())
                .recordatorioComidas(u.getRecordatorioComidas())
                .rangoGlucosaMin(u.getRangoGlucosaMin())
                .rangoGlucosaMax(u.getRangoGlucosaMax())
                .zonaHoraria(u.getZonaHoraria())
                .rol(u.getRol())
                .build();
    }
}
