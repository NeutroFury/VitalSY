package com.jonesys.vitalsy.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutricionRequest {
    
    @Size(max = 255, message = "Descripción no debe exceder 255 caracteres")
    private String descripcionComida;
    
    @NotNull(message = "Carbohidratos es requerido")
    @DecimalMin(value = "0.0", message = "Carbohidratos debe ser mayor o igual a 0")
    @DecimalMax(value = "999.99", message = "Carbohidratos no debe exceder 999.99")
    private Double carbohidratosGr;
    
    @DecimalMin(value = "0.0", message = "Proteínas debe ser mayor o igual a 0")
    @DecimalMax(value = "999.99", message = "Proteínas no debe exceder 999.99")
    private Double proteinasGr;
    
    @DecimalMin(value = "0.0", message = "Grasas debe ser mayor o igual a 0")
    @DecimalMax(value = "999.99", message = "Grasas no debe exceder 999.99")
    private Double grasasGr;
    
    @Min(value = 0, message = "Calorías debe ser mayor o igual a 0")
    @Max(value = 5000, message = "Calorías no debe exceder 5000")
    private Integer caloriasKcal;
    
    @Pattern(regexp = "^(Desayuno|Almuerzo|Cena|Snack)$", message = "Momento del día inválido")
    private String momentoDia;
    
    @Size(max = 50, message = "Estado de ánimo no debe exceder 50 caracteres")
    private String estadoAnimo;
    
    @Size(max = 255, message = "URL de foto no debe exceder 255 caracteres")
    @Pattern(regexp = "^(https?://.*)?$", message = "URL de foto debe ser válida")
    private String fotoUrl;
    
    private ZonedDateTime fechaHora;
}
