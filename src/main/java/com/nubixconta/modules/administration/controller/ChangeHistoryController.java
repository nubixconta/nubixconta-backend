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
     * Obtiene todas las entradas de un usuario, o si se envían 'start' y 'end',
     * solo las de ese rango.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChangeHistory>> getChangesByUser(
            @PathVariable Integer userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end) {

        List<ChangeHistory> result;
        if (start != null && end != null) {
            result = changeHistoryService.getChangesByUserAndDateBetween(userId, start, end);
        } else {
            result = changeHistoryService.getAllChangesByUserOrdered(userId);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Obtiene solo las entradas de un usuario que NO tienen empresa asociada.
     */
    @GetMapping("/user/{userId}/without-company")
    public ResponseEntity<List<ChangeHistory>> getChangesWithoutCompany(
            @PathVariable Integer userId) {

        List<ChangeHistory> result = changeHistoryService.getChangesWithoutCompany(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Obtiene todas las entradas en un rango de fechas, independientemente de usuario.
     */
    @GetMapping("/dates")
    public ResponseEntity<List<ChangeHistory>> getByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end) {

        return ResponseEntity.ok(
                changeHistoryService.getByDateRange(start, end)
        );
    }
}
