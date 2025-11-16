package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.dto.reports.CierreMensualStatusDTO;
import com.nubixconta.modules.accounting.service.CierreContableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Opcional para seguridad futura
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cierres")
@RequiredArgsConstructor
public class CierreContableController {

    private final CierreContableService cierreService;

    /**
     * Endpoint para marcar un mes como cerrado.
     */
    @PostMapping("/mensual/{anio}/{mes}")
    // @PreAuthorize("hasRole('ADMIN_CONTABLE')") // A futuro, proteger con un rol
    public ResponseEntity<Void> cerrarMes(@PathVariable int anio, @PathVariable int mes) {
        cierreService.cerrarMes(anio, mes);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint para reabrir un mes previamente cerrado.
     */
    @DeleteMapping("/mensual/{anio}/{mes}")
    // @PreAuthorize("hasRole('ADMIN_CONTABLE')") // A futuro, proteger con un rol especial
    public ResponseEntity<Void> reabrirMes(@PathVariable int anio, @PathVariable int mes) {
        cierreService.reabrirMes(anio, mes);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint para obtener el estado (abierto/cerrado) de todos los meses de un año.
     */
    @GetMapping("/mensual/{anio}")
    public ResponseEntity<List<CierreMensualStatusDTO>> getEstadosDeCierrePorAnio(@PathVariable int anio) {
        return ResponseEntity.ok(cierreService.getEstadosDeCierre(anio));
    }
}