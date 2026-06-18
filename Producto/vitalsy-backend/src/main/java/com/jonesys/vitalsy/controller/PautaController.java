package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.service.PautaExcelService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pautas")
public class PautaController {

    private final PautaExcelService pautaExcelService;

    public PautaController(PautaExcelService pautaExcelService) {
        this.pautaExcelService = pautaExcelService;
    }

    @PostMapping(value = "/upload/{usuarioId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPautaExcel(@PathVariable Integer usuarioId,
                                              @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El archivo está vacío."));
        }

        try {
            int registrosInsertados = pautaExcelService.procesarPautaExcel(usuarioId, file);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Pauta médica procesada e inyectada con éxito.",
                    "registrosInyectados", registrosInsertados
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensaje", "Error de Validación en el Excel: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "mensaje", "Error interno procesando el Excel: " + e.getMessage()
            ));
        }
    }
}
