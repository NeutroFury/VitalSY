package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.request.RecordatorioRequest;
import com.jonesys.vitalsy.dto.response.RecordatorioResponse;
import com.jonesys.vitalsy.service.RecordatorioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recordatorios")
@RequiredArgsConstructor
public class RecordatorioController {

    private final RecordatorioService recordatorioService;

    @GetMapping
    public ResponseEntity<List<RecordatorioResponse>> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(recordatorioService.getAll(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<RecordatorioResponse> create(@AuthenticationPrincipal UserDetails userDetails,
                                                       @Valid @RequestBody RecordatorioRequest request) {
        return ResponseEntity.ok(recordatorioService.create(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecordatorioResponse> update(@AuthenticationPrincipal UserDetails userDetails,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody RecordatorioRequest request) {
        return ResponseEntity.ok(recordatorioService.update(userDetails.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long id) {
        recordatorioService.delete(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
