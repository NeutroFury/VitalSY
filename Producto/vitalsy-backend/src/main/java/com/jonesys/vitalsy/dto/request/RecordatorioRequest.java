package com.jonesys.vitalsy.dto.request;

import com.jonesys.vitalsy.model.TipoRecordatorio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@NoArgsConstructor
public class RecordatorioRequest {

    @NotNull(message = "El tipo es requerido")
    private TipoRecordatorio tipo;

    @NotNull(message = "La hora es requerida")
    private LocalTime hora;

    @NotBlank(message = "Los días de repetición son requeridos")
    private String diasRepeticion;

    private Boolean activo = true;
}
