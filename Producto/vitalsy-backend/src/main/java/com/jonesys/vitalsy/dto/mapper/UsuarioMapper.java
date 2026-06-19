package com.jonesys.vitalsy.dto.mapper;

import com.jonesys.vitalsy.dto.response.UsuarioResponse;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario u) {
        if (u == null) {
            return null;
        }
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

                .rangoGlucosaMin(u.getRangoGlucosaMin())
                .rangoGlucosaMax(u.getRangoGlucosaMax())
                .zonaHoraria(u.getZonaHoraria())
                .rol(u.getRol())
                .activo(u.getActivo())
                .build();
    }
}
