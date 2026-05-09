package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.request.LoginRequest;
import com.jonesys.vitalsy.dto.request.RegisterRequest;
import com.jonesys.vitalsy.dto.response.LoginResponse;
import com.jonesys.vitalsy.dto.response.RegisterResponse;
import com.jonesys.vitalsy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:8100")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse resp = authService.register(request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse resp = authService.login(request);
        return ResponseEntity.ok(resp);
    }
}
