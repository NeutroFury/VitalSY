package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.response.UsuarioResponse;
import com.jonesys.vitalsy.service.AdminService;
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

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * GET /api/v1/admin/usuarios
     * Devuelve la lista de todos los pacientes registrados.
     * Requiere rol ADMIN.
     */
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponse>> getAllPacientes(Authentication authentication) {
        List<UsuarioResponse> pacientes = adminService.getAllPacientes(authentication.getName());
        return ResponseEntity.ok(pacientes);
    }

    /**
     * GET /api/v1/admin/usuarios/todos
     * Devuelve todos los usuarios incluyendo admins (para auditoría).
     * Requiere rol ADMIN.
     */
    @GetMapping("/usuarios/todos")
    public ResponseEntity<List<UsuarioResponse>> getAllUsuarios(Authentication authentication) {
        List<UsuarioResponse> todos = adminService.getAllUsuarios(authentication.getName());
        return ResponseEntity.ok(todos);
    }

    /**
     * GET /api/v1/admin/usuarios/{id}
     * Devuelve el detalle de un usuario específico.
     * Requiere rol ADMIN.
     */
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> getUsuarioById(@PathVariable Integer id, Authentication authentication) {
        UsuarioResponse usuario = adminService.getUsuarioById(id, authentication.getName());
        return ResponseEntity.ok(usuario);
    }

    /**
     * PUT /api/v1/admin/usuarios/{id}/rol
     * Cambia el rol de un usuario. Body: { "rol": "ADMIN" | "PACIENTE" | "MEDICO" }
     * Requiere rol ADMIN.
     */
    @PutMapping("/usuarios/{id}/rol")
    public ResponseEntity<Map<String, String>> cambiarRol(@PathVariable Integer id,
                                         @RequestBody Map<String, String> body,
                                         Authentication authentication) {
        Map<String, String> response = adminService.cambiarRol(id, body.get("rol"), authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/admin/stats
     * Estadísticas básicas del sistema.
     * Requiere rol ADMIN.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        Map<String, Object> stats = adminService.getStats(authentication.getName());
        return ResponseEntity.ok(stats);
    }

    /**
     * PATCH /api/v1/admin/usuarios/{id}/toggle-status
     * Invierte el estado activo/inactivo del usuario.
     */
    @PatchMapping("/usuarios/{id}/toggle-status")
    public ResponseEntity<Map<String, String>> toggleStatus(@PathVariable Integer id, Authentication authentication) {
        Map<String, String> response = adminService.toggleUserStatus(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/admin/usuarios/{id}/trigger-password-reset
     * Envía un enlace de recuperación de contraseña al usuario.
     */
    @PostMapping("/usuarios/{id}/trigger-password-reset")
    public ResponseEntity<Map<String, String>> triggerPasswordReset(@PathVariable Integer id, Authentication authentication) {
        Map<String, String> response = adminService.triggerPasswordReset(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
