package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.dto.catalog.*;
import com.nubixconta.modules.accounting.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // --- ¡INICIO DE NUEVOS ENDPOINTS DE GESTIÓN! ---

    /**
     * Devuelve el árbol del catálogo personalizado para la empresa del usuario autenticado.
     */
    @GetMapping("/my-company/tree")
    public ResponseEntity<List<CompanyCatalogNodeDTO>> getMyCompanyCatalogTree() {
        return ResponseEntity.ok(catalogService.getCompanyTree());
    }

    /**
     * Activa una o más cuentas del catálogo maestro para la empresa actual.
     */
    @PostMapping("/activate")
    public ResponseEntity<Void> activateAccounts(@Valid @RequestBody ActivateAccountsDTO dto) {
        catalogService.activateAccounts(dto.getMasterAccountIds());
        return ResponseEntity.ok().build();
    }

    /**
     * Desactiva una o más cuentas del catálogo de la empresa actual, incluyendo sus descendientes.
     */
    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateAccounts(@Valid @RequestBody DeactivateAccountsDTO dto) {
        catalogService.deactivateAccounts(dto.getCatalogIds());
        return ResponseEntity.ok().build();
    }
    /**
     * Actualiza el nombre y/o código personalizado de una cuenta del catálogo de la empresa.
     */
    @PutMapping("/{catalogId}")
    public ResponseEntity<CompanyCatalogNodeDTO> updateCatalog(
            @PathVariable Integer catalogId,
            @RequestBody UpdateCatalogDTO dto) {
        CompanyCatalogNodeDTO updatedNode = catalogService.updateCustomFields(catalogId, dto);
        return ResponseEntity.ok(updatedNode);
    }
    /**
     * Activa y personaliza masivamente el catálogo de la empresa actual
     * a partir de un archivo Excel.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadCompanyCatalog(@RequestParam("file") MultipartFile file) {
        try {
            catalogService.processCompanyCatalogUpload(file);
            return ResponseEntity.ok("Catálogo de la empresa cargado y personalizado exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar el archivo: " + e.getMessage());
        }
    }
}