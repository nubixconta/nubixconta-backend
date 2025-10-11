package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.dto.catalog.CatalogSummaryDTO;
import com.nubixconta.modules.accounting.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogs")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    /**
     * Endpoint para buscar en el catálogo de cuentas de la empresa actual.
     * El frontend lo usará para el campo 'Buscar Cuenta Contable'.
     *
     * @param term El término de búsqueda (opcional, puede ser el nombre o el código de la cuenta).
     * @return Una lista de cuentas que coinciden.
     */
    @GetMapping("/search")
    public ResponseEntity<List<CatalogSummaryDTO>> searchCatalogs(@RequestParam(name = "term", required = false) String term) {
        List<CatalogSummaryDTO> results = catalogService.searchActiveByTerm(term);
        return ResponseEntity.ok(results);
    }
}