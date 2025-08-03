package com.nubixconta.modules.administration.controller;

import com.nubixconta.modules.administration.dto.changehistory.ChangeHistoryCreateDTO;
import com.nubixconta.modules.administration.dto.changehistory.ChangeHistoryResponseDTO;
import com.nubixconta.modules.administration.entity.ChangeHistory;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/change-history")
public class ChangeHistoryController {

    @Autowired
    private ChangeHistoryService changeHistoryService;

    @GetMapping
    public ResponseEntity<List<ChangeHistoryResponseDTO>> getAllSummarizedHistory() {
        List<ChangeHistoryResponseDTO> result = changeHistoryService.getAllHistoryResponses();
        return ResponseEntity.ok(result);
    }
    /**
     * Registra manualmente una entrada en la bitácora de cambios.
     */
    @PostMapping("/log")
    public ResponseEntity<Void> logChange(@Valid @RequestBody ChangeHistoryCreateDTO dto) {
        changeHistoryService.logChange(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Obtiene todas las entradas de la bitácora, opcionalmente filtradas por un rango de fechas.
     */
    @GetMapping("/by-dates")
    public ResponseEntity<List<ChangeHistoryResponseDTO>> getChangesByDateRange(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end) {

        List<ChangeHistoryResponseDTO> result = changeHistoryService.getChangesByDateRange(start, end);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChangeHistoryResponseDTO>> getChangesByUser(
            @PathVariable Integer userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end) {

        List<ChangeHistoryResponseDTO> result = changeHistoryService.getChangesByUserFiltered(userId, start, end);
        return ResponseEntity.ok(result);
    }
}
