package com.jonesys.vitalsy.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsulinaRequest {
    
    @DecimalMin(value = "0.0", message = "Unidades sugeridas debe ser mayor o igual a 0")
    @DecimalMax(value = "999.99", message = "Unidades sugeridas no debe exceder 999.99")
    private Double unidadesSugeridas;
    
    @NotNull(message = "Unidades aplicadas es requerido")
    @DecimalMin(value = "0.0", message = "Unidades aplicadas debe ser mayor o igual a 0")
    @DecimalMax(value = "999.99", message = "Unidades aplicadas no debe exceder 999.99")
    private Double unidadesAplicadas;
    
    @Pattern(regexp = "^(Rápida|Basal)$", message = "Tipo de insulina debe ser: Rápida o Basal")
    private String tipoInsulina;
    
    @Pattern(regexp = "^(Abdomen|Brazo|Muslo)$", message = "Sitio de aplicación debe ser: Abdomen, Brazo o Muslo")
    private String sitioAplicacion;
    
    private Integer glucoseReadingId;
    
    private Integer registroNutricionId;
    
    private ZonedDateTime fechaHora;
}
