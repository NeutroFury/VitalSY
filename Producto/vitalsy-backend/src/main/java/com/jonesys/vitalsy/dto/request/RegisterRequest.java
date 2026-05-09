package com.jonesys.vitalsy.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    private String email;
    
    @NotBlank(message = "Contraseña es requerida")
    @Size(min = 6, message = "Contraseña debe tener al menos 6 caracteres")
    private String password;
    
    @NotBlank(message = "Nombre es requerido")
    @Size(min = 2, max = 100, message = "Nombre debe estar entre 2 y 100 caracteres")
    private String nombre;
    
    @Past(message = "Fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;
    
    @Pattern(regexp = "^(Masculino|Femenino|Otro)$", message = "Género debe ser: Masculino, Femenino u Otro")
    private String genero;
    
    @DecimalMin(value = "20.0", message = "Peso debe ser mayor a 20 kg")
    @DecimalMax(value = "500.0", message = "Peso debe ser menor a 500 kg")
    private Double pesoActual;
}
