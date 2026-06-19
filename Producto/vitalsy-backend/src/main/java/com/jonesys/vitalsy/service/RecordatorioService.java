package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.dto.request.RecordatorioRequest;
import com.jonesys.vitalsy.dto.response.RecordatorioResponse;
import com.jonesys.vitalsy.model.Recordatorio;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.RecordatorioRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RecordatorioService {

    private final RecordatorioRepository recordatorioRepository;
    private final UsuarioRepository usuarioRepository;

    public List<RecordatorioResponse> getAll(String email) {
        log.debug("Obteniendo todos los recordatorios para: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);
        return recordatorioRepository.findByUsuario(usuario)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RecordatorioResponse create(String email, RecordatorioRequest request) {
        log.debug("Creando nuevo recordatorio para: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);
        
        Recordatorio r = new Recordatorio();
        r.setUsuario(usuario);
        r.setTipo(request.getTipo());
        r.setHora(request.getHora());
        r.setDiasRepeticion(request.getDiasRepeticion());
        r.setActivo(request.getActivo() != null ? request.getActivo() : true);
        
        Recordatorio saved = recordatorioRepository.save(r);
        return toResponse(saved);
    }

    public RecordatorioResponse update(String email, Long id, RecordatorioRequest request) {
        log.debug("Actualizando recordatorio ID {} para: {}", id, email);
        Recordatorio r = getRecordatorioOrThrow(id, email);
        
        if (request.getTipo() != null) r.setTipo(request.getTipo());
        if (request.getHora() != null) r.setHora(request.getHora());
        if (request.getDiasRepeticion() != null) r.setDiasRepeticion(request.getDiasRepeticion());
        if (request.getActivo() != null) r.setActivo(request.getActivo());
        
        Recordatorio updated = recordatorioRepository.save(r);
        return toResponse(updated);
    }

    public void delete(String email, Long id) {
        log.debug("Eliminando recordatorio ID {} para: {}", id, email);
        Recordatorio r = getRecordatorioOrThrow(id, email);
        recordatorioRepository.delete(r);
    }

    private Usuario getUsuarioOrThrow(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private Recordatorio getRecordatorioOrThrow(Long id, String email) {
        Recordatorio r = recordatorioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recordatorio no encontrado"));
        if (!r.getUsuario().getEmail().equals(email)) {
            throw new RuntimeException("No autorizado para acceder a este recordatorio");
        }
        return r;
    }

    private RecordatorioResponse toResponse(Recordatorio r) {
        return new RecordatorioResponse(
                r.getId(),
                r.getTipo(),
                r.getHora(),
                r.getDiasRepeticion(),
                r.getActivo(),
                r.getFechaCreacion()
        );
    }
}
