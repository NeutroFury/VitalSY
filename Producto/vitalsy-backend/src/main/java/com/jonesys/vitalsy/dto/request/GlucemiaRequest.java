package com.jonesys.vitalsy.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlucemiaRequest {
    
    @NotNull(message = "Valor de glucemia es requerido")
    @Min(value = 20, message = "Valor debe ser mayor a 20 mg/dL")
    @Max(value = 600, message = "Valor debe ser menor a 600 mg/dL")
    private Integer valorMgdl;
    
    @NotBlank(message = "Tipo de registro es requerido")
    @Pattern(regexp = "^(MANUAL|SENSOR_NFC|SENSOR_BLE)$", 
             message = "Tipo debe ser: MANUAL, SENSOR_NFC o SENSOR_BLE")
    private String tipoRegistro;
    
    @Pattern(regexp = "^(Stable|Rising|Falling|RapidlyRising|RapidlyFalling|DoubleUp|DoubleDown)$",
             message = "Tendencia inválida")
    private String tendencia;
    
    @Size(max = 100, message = "ID de dispositivo no debe exceder 100 caracteres")
    private String dispositivoId;
    
    @Size(max = 500, message = "Comentarios no debe exceder 500 caracteres")
    private String comentarios;
    
    private ZonedDateTime fechaHora;
}
