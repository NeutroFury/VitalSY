package com.jonesys.vitalsy.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametrosClinicosDTO {
    
    @NotNull(message = "El rango de glucosa mínimo es requerido")
    private Integer rangoGlucosaMin;
    
    @NotNull(message = "El rango de glucosa máximo es requerido")
    private Integer rangoGlucosaMax;
}
