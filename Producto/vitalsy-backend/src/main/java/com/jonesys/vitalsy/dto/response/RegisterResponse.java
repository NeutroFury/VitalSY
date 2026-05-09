package com.jonesys.vitalsy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    
    private Integer id;
    private String email;
    private String nombre;
    private ZonedDateTime creadoEn;
}
