package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.dto.request.LoginRequest;
import com.jonesys.vitalsy.dto.request.RegisterRequest;
import com.jonesys.vitalsy.dto.response.LoginResponse;
import com.jonesys.vitalsy.dto.response.RegisterResponse;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.security.JwtProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Collections;

@Service
public class AuthService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public RegisterResponse register(RegisterRequest req) {
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email ya registrado");
        }

        Usuario u = new Usuario();
        u.setEmail(req.getEmail());
        u.setNombre(req.getNombre());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setFechaNacimiento(req.getFechaNacimiento());
        u.setGenero(req.getGenero());
        u.setPesoActual(req.getPesoActual());
        u.setRol("PACIENTE"); // Rol por defecto
        u.setActivo(true);   // Usuario activo por defecto
        u.setCreadoEn(ZonedDateTime.now());

        Usuario saved = usuarioRepository.save(u);

        return new RegisterResponse(saved.getId(), saved.getEmail(), saved.getNombre(), saved.getCreadoEn());
    }

    public LoginResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(req.getPassword(), usuario.getPasswordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Credenciales inválidas");
        }

        String token = jwtProvider.generateToken(usuario.getEmail(), usuario.getId());

        int expiresInSeconds = (int) (jwtProvider.getExpirationMs() / 1000);

        return new LoginResponse(token, usuario.getId(), usuario.getEmail(), usuario.getNombre(), expiresInSeconds, "Bearer");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .authorities("ROLE_" + usuario.getRol()) // Aseguramos el prefijo ROLE_
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!usuario.getActivo())
                .build();
    }
}
