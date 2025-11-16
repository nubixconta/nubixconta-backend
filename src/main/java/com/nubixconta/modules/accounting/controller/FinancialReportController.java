package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.dto.reports.*;
import com.nubixconta.modules.accounting.entity.GeneralLedgerView;
import com.nubixconta.modules.accounting.service.FinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class FinancialReportController {

    private final FinancialReportService reportService;

    @GetMapping("/libro-diario")
    public ResponseEntity<List<LibroDiarioMovimientoDTO>> getLibroDiario( // <-- CAMBIO AQUÍ
                                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(reportService.getLibroDiario(startDate, endDate));
    }
    @GetMapping("/libro-mayor")
    public ResponseEntity<List<LibroMayorCuentaDTO>> getLibroMayor(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer catalogId) { // Parámetro de cuenta opcional

        // Añadimos una validación para asegurar que al menos un filtro esté presente
        if (startDate == null && endDate == null && catalogId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe proporcionar al menos un filtro: rango de fechas o ID de cuenta.");
        }

        return ResponseEntity.ok(reportService.getLibroMayor(startDate, endDate, catalogId));
    }

    @GetMapping("/balanza-comprobacion")
    public ResponseEntity<List<BalanzaComprobacionLineaDTO>> getBalanzaDeComprobacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(reportService.getBalanzaDeComprobacion(startDate, endDate));
    }

    @GetMapping("/estado-resultados")
    public ResponseEntity<EstadoResultadosResponseDTO> getEstadoDeResultados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(reportService.getEstadoDeResultados(startDate, endDate));
    }

    @GetMapping("/balance-general")
    public ResponseEntity<BalanceGeneralResponseDTO> getBalanceGeneral(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // El Balance General es una foto a una fecha de corte. Solo necesita la fecha final.
        return ResponseEntity.ok(reportService.getBalanceGeneral(endDate));
    }

}