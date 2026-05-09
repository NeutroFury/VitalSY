package com.jonesys.vitalsy.dto.mapper;

import com.jonesys.vitalsy.dto.request.RegisterRequest;
import com.jonesys.vitalsy.dto.response.LoginResponse;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioDTOMapper {
    
    public Usuario toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setGenero(request.getGenero());
        usuario.setPesoActual(request.getPesoActual());
        usuario.setActivo(true);
        usuario.setRol("PACIENTE");
        
        return usuario;
    }
    
    public LoginResponse toLoginResponse(Usuario usuario, String token, Integer expiresIn) {
        if (usuario == null) {
            return null;
        }
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setNombre(usuario.getNombre());
        response.setExpiresIn(expiresIn);
        response.setTokenType("Bearer");
        
        return response;
    }
}
