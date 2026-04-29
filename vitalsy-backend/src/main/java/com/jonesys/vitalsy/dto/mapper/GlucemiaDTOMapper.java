package com.jonesys.vitalsy.dto.mapper;

import com.jonesys.vitalsy.dto.request.GlucemiaRequest;
import com.jonesys.vitalsy.dto.response.GlucemiaResponse;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;

@Component
public class GlucemiaDTOMapper {
    
    public GlucoseReading toEntity(GlucemiaRequest request, Usuario usuario) {
        if (request == null) {
            return null;
        }
        
        GlucoseReading glucoseReading = new GlucoseReading();
        glucoseReading.setUsuario(usuario);
        glucoseReading.setValorMgdl(request.getValorMgdl());
        glucoseReading.setTipoRegistro(request.getTipoRegistro());
        glucoseReading.setTendencia(request.getTendencia());
        glucoseReading.setDispositivoId(request.getDispositivoId());
        glucoseReading.setComentarios(request.getComentarios());
        
        if (request.getFechaHora() != null) {
            glucoseReading.setFechaHora(request.getFechaHora());
        } else {
            glucoseReading.setFechaHora(ZonedDateTime.now());
        }
        
        return glucoseReading;
    }
    
    public GlucemiaResponse toResponse(GlucoseReading glucoseReading) {
        if (glucoseReading == null) {
            return null;
        }
        
        GlucemiaResponse response = new GlucemiaResponse();
        response.setId(glucoseReading.getId());
        response.setUsuarioId(glucoseReading.getUsuario().getId());
        response.setValorMgdl(glucoseReading.getValorMgdl());
        response.setTipoRegistro(glucoseReading.getTipoRegistro());
        response.setTendencia(glucoseReading.getTendencia());
        response.setDispositivoId(glucoseReading.getDispositivoId());
        response.setComentarios(glucoseReading.getComentarios());
        response.setAnalisisIa(glucoseReading.getAnalisisIa());
        response.setFechaHora(glucoseReading.getFechaHora());
        response.setCreadoEn(glucoseReading.getCreadoEn());
        
        return response;
    }
}
