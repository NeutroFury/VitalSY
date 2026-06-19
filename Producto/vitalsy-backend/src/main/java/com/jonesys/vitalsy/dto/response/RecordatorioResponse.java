package com.jonesys.vitalsy.dto.response;

import com.jonesys.vitalsy.model.TipoRecordatorio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioResponse {
    private Long id;
    private TipoRecordatorio tipo;
    private LocalTime hora;
    private String diasRepeticion;
    private Boolean activo;
    private ZonedDateTime fechaCreacion;
}
