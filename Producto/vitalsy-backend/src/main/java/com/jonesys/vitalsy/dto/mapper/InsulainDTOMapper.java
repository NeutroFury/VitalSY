package com.jonesys.vitalsy.dto.mapper;

import com.jonesys.vitalsy.dto.request.InsulinaRequest;
import com.jonesys.vitalsy.dto.response.InsulinaResponse;
import com.jonesys.vitalsy.model.RegistroInsulina;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.RegistroNutricion;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;

@Component
public class InsulainDTOMapper {
    
    public RegistroInsulina toEntity(InsulinaRequest request, Usuario usuario, 
                                    GlucoseReading glucoseReading, RegistroNutricion nutricion) {
        if (request == null) {
            return null;
        }
        
        RegistroInsulina insulina = new RegistroInsulina();
        insulina.setUsuario(usuario);
        insulina.setUnidadesSugeridas(request.getUnidadesSugeridas());
        insulina.setUnidadesAplicadas(request.getUnidadesAplicadas());
        insulina.setTipoInsulina(request.getTipoInsulina());
        insulina.setSitioAplicacion(request.getSitioAplicacion());
        insulina.setGlucoseReading(glucoseReading);
        insulina.setRegistroNutricion(nutricion);
        
        if (request.getFechaHora() != null) {
            insulina.setFechaHora(request.getFechaHora());
        } else {
            insulina.setFechaHora(ZonedDateTime.now());
        }
        
        return insulina;
    }
    
    public InsulinaResponse toResponse(RegistroInsulina insulina) {
        if (insulina == null) {
            return null;
        }
        
        InsulinaResponse response = new InsulinaResponse();
        response.setId(insulina.getId());
        response.setUsuarioId(insulina.getUsuario().getId());
        response.setUnidadesSugeridas(insulina.getUnidadesSugeridas());
        response.setUnidadesAplicadas(insulina.getUnidadesAplicadas());
        response.setTipoInsulina(insulina.getTipoInsulina());
        response.setSitioAplicacion(insulina.getSitioAplicacion());
        
        if (insulina.getGlucoseReading() != null) {
            response.setGlucoseReadingId(insulina.getGlucoseReading().getId());
        }
        
        if (insulina.getRegistroNutricion() != null) {
            response.setRegistroNutricionId(insulina.getRegistroNutricion().getId());
        }
        
        response.setFechaHora(insulina.getFechaHora());
        
        return response;
    }
}
