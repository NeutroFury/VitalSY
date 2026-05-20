package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.response.UsuarioResponse;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:8100")
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> getPerfil(Authentication authentication) {
        System.out.println("DEBUG: Petición GET perfil para: " + authentication.getName());
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + authentication.getName()));

        return ResponseEntity.ok(mapToResponse(usuario));
    }

    @PutMapping("/perfil")
    public ResponseEntity<UsuarioResponse> updatePerfil(@RequestBody UsuarioResponse request, Authentication authentication) {
        System.out.println("DEBUG: Petición PUT perfil para: " + authentication.getName());
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + authentication.getName()));

        usuario.setPesoActual(request.getPesoActual());
        usuario.setAltura(request.getAltura());
        usuario.setTipoInsulina(request.getTipoInsulina());
        usuario.setRatioIc(request.getRatioIc());
        usuario.setFactorIs(request.getFactorIs());
        usuario.setAlertasGlucosa(request.getAlertasGlucosa());
        usuario.setRecordatorioComidas(request.getRecordatorioComidas());

        Usuario saved = usuarioRepository.save(usuario);
        System.out.println("DEBUG: Perfil actualizado con éxito para: " + saved.getEmail());
        return ResponseEntity.ok(mapToResponse(saved));
    }

    private UsuarioResponse mapToResponse(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .email(u.getEmail())
                .pesoActual(u.getPesoActual())
                .altura(u.getAltura())
                .tipoInsulina(u.getTipoInsulina())
                .ratioIc(u.getRatioIc())
                .factorIs(u.getFactorIs())
                .alertasGlucosa(u.getAlertasGlucosa())
                .recordatorioComidas(u.getRecordatorioComidas())
                .build();
    }
}
