package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.EscalaDosisFijaAiDTO;
import com.jonesys.vitalsy.model.EscalaDosisFija;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.EscalaDosisFijaRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.service.MotorCognitivoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cognitivo")
public class MotorCognitivoController {

    private final MotorCognitivoService motorCognitivoService;
    private final UsuarioRepository usuarioRepository;
    private final EscalaDosisFijaRepository escalaDosisFijaRepository;

    public MotorCognitivoController(MotorCognitivoService motorCognitivoService,
                                    UsuarioRepository usuarioRepository,
                                    EscalaDosisFijaRepository escalaDosisFijaRepository) {
        this.motorCognitivoService = motorCognitivoService;
        this.usuarioRepository = usuarioRepository;
        this.escalaDosisFijaRepository = escalaDosisFijaRepository;
    }

    @PostMapping(value = "/pauta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirPautaMedica(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {
        
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        try {
            List<EscalaDosisFija> extraidas = motorCognitivoService.procesarPautaMedica(files, usuario.getId());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Pauta médica procesada con éxito por Gemini.",
                    "reglasExtraidas", extraidas.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fallo al procesar la pauta: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/pauta/mock")
    @Transactional
    public ResponseEntity<?> inyectarPautaMock(
            @RequestBody List<EscalaDosisFijaAiDTO> dtos,
            Authentication authentication) {
        
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Limpiar cargas viejas
        escalaDosisFijaRepository.deleteAllByUsuario_Id(usuario.getId());

        // Mapear e insertar en masa
        List<EscalaDosisFija> entidades = dtos.stream().map(dto -> {
            EscalaDosisFija entity = new EscalaDosisFija();
            entity.setUsuario(usuario);
            entity.setNombreComidaPersonalizado(dto.momentoDia());
            entity.setGlicemiaMin(dto.glicemiaMin());
            entity.setGlicemiaMax(dto.glicemiaMax());
            entity.setCarbohidratosGr(dto.carbohidratosGr());
            entity.setDosisInsulina(dto.dosisInsulina());
            return entity;
        }).collect(Collectors.toList());

        escalaDosisFijaRepository.saveAll(entidades);

        return ResponseEntity.ok(Map.of("mensaje", "¡Pauta Médica Completa Inyectada en Supabase!"));
    }
}
