package com.jonesys.vitalsy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private String token;
    private Integer userId;
    private String email;
    private String nombre;
    private Integer expiresIn; // Segundos
    private String tokenType = "Bearer";
}
