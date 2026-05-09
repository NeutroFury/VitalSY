package com.jonesys.vitalsy.dto.mapper;

import com.jonesys.vitalsy.dto.request.NutricionRequest;
import com.jonesys.vitalsy.dto.response.NutricionResponse;
import com.jonesys.vitalsy.model.RegistroNutricion;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;

@Component
public class NutricionDTOMapper {
    
    public RegistroNutricion toEntity(NutricionRequest request, Usuario usuario) {
        if (request == null) {
            return null;
        }
        
        RegistroNutricion nutricion = new RegistroNutricion();
        nutricion.setUsuario(usuario);
        nutricion.setDescripcionComida(request.getDescripcionComida());
        nutricion.setCarbohidratosGr(request.getCarbohidratosGr());
        nutricion.setProteinasGr(request.getProteinasGr());
        nutricion.setGrasasGr(request.getGrasasGr());
        nutricion.setCaloriasKcal(request.getCaloriasKcal());
        nutricion.setMomentoDia(request.getMomentoDia());
        nutricion.setEstadoAnimo(request.getEstadoAnimo());
        nutricion.setFotoUrl(request.getFotoUrl());
        
        if (request.getFechaHora() != null) {
            nutricion.setFechaHora(request.getFechaHora());
        } else {
            nutricion.setFechaHora(ZonedDateTime.now());
        }
        
        return nutricion;
    }
    
    public NutricionResponse toResponse(RegistroNutricion nutricion) {
        if (nutricion == null) {
            return null;
        }
        
        NutricionResponse response = new NutricionResponse();
        response.setId(nutricion.getId());
        response.setUsuarioId(nutricion.getUsuario().getId());
        response.setDescripcionComida(nutricion.getDescripcionComida());
        response.setCarbohidratosGr(nutricion.getCarbohidratosGr());
        response.setProteinasGr(nutricion.getProteinasGr());
        response.setGrasasGr(nutricion.getGrasasGr());
        response.setCaloriasKcal(nutricion.getCaloriasKcal());
        response.setMomentoDia(nutricion.getMomentoDia());
        response.setEstadoAnimo(nutricion.getEstadoAnimo());
        response.setFotoUrl(nutricion.getFotoUrl());
        response.setFechaHora(nutricion.getFechaHora());
        
        return response;
    }
}
