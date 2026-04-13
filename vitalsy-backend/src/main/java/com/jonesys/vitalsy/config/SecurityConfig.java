package com.jonesys.vitalsy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Requerido para APIs REST [cite: 135]
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Política
                                                                                                              // definida
                                                                                                              // en tu
                                                                                                              // informe
                                                                                                              // [cite:
                                                                                                              // 117]
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll() // Para login y registro [cite: 131]
                        .requestMatchers("/api/v1/glucosa/**").permitAll() // Para el CRUD base [cite: 132]
                        .requestMatchers("/api/v1/ia/**").permitAll() // 👈 AGREGA ESTA LÍNEA para habilitar el Engine
                                                                      // de IA [cite: 133]
                        .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Implementamos el cifrado definido en el informe [cite: 117, 131]
    }
}