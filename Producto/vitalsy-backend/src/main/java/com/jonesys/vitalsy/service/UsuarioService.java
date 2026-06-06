package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.dto.mapper.UsuarioMapper;
import com.jonesys.vitalsy.dto.request.ParametrosClinicosDTO;
import com.jonesys.vitalsy.dto.response.UsuarioResponse;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
    }

    public UsuarioResponse getPerfil(String email) {
        log.debug("Obteniendo perfil para el usuario: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);
        return usuarioMapper.toResponse(usuario);
    }

    public UsuarioResponse updatePerfil(String email, UsuarioResponse request) {
        log.debug("Actualizando perfil para el usuario: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            usuario.setNombre(request.getNombre());
        }
        if (request.getPesoActual() != null) {
            usuario.setPesoActual(request.getPesoActual());
        }
        if (request.getAltura() != null) {
            usuario.setAltura(request.getAltura());
        }
        if (request.getInsulinaLenta() != null) {
            usuario.setInsulinaLenta(request.getInsulinaLenta());
        }
        if (request.getInsulinaRapida() != null) {
            usuario.setInsulinaRapida(request.getInsulinaRapida());
        }
        if (request.getRatioIc() != null) {
            usuario.setRatioIc(request.getRatioIc());
        }
        if (request.getFactorIs() != null) {
            usuario.setFactorIs(request.getFactorIs());
        }
        if (request.getAlertasGlucosa() != null) {
            usuario.setAlertasGlucosa(request.getAlertasGlucosa());
        }
        if (request.getRecordatorioComidas() != null) {
            usuario.setRecordatorioComidas(request.getRecordatorioComidas());
        }
        if (request.getZonaHoraria() != null && !request.getZonaHoraria().isBlank()) {
            usuario.setZonaHoraria(request.getZonaHoraria());
        }

        Usuario saved = usuarioRepository.save(usuario);
        log.debug("Perfil actualizado con éxito para: {}", saved.getEmail());
        return usuarioMapper.toResponse(saved);
    }

    public UsuarioResponse updateParametrosClinicos(String email, ParametrosClinicosDTO request) {
        log.debug("Actualizando parámetros clínicos para el usuario: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);

        if (request.getRangoGlucosaMin() > request.getRangoGlucosaMax()) {
            throw new RuntimeException("El rango mínimo no puede ser mayor que el rango máximo.");
        }

        usuario.setRangoGlucosaMin(request.getRangoGlucosaMin());
        usuario.setRangoGlucosaMax(request.getRangoGlucosaMax());

        Usuario saved = usuarioRepository.save(usuario);
        log.debug("Parámetros clínicos actualizados con éxito para: {}", saved.getEmail());
        return usuarioMapper.toResponse(saved);
    }

    private Usuario getUsuarioOrThrow(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }
}
