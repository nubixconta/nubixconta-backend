package com.nubixconta.modules.purchases.controller;

import com.nubixconta.common.exception.BadRequestException;
import com.nubixconta.modules.accounting.dto.AccountingEntryResponseDTO;
import com.nubixconta.modules.accounting.service.PurchasesAccountingService;
import com.nubixconta.modules.purchases.dto.purchases.PurchaseCreateDTO;
import com.nubixconta.modules.purchases.dto.purchases.PurchaseForCreditNoteDTO;
import com.nubixconta.modules.purchases.dto.purchases.PurchaseResponseDTO;
import com.nubixconta.modules.purchases.dto.purchases.PurchaseUpdateDTO;
import com.nubixconta.modules.purchases.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final PurchasesAccountingService purchasesAccountingService;

    /**
     * Obtiene todas las compras registradas con un ordenamiento específico.
     * @param sortBy (opcional) Criterio de ordenamiento.
     *               - 'status' (default): Agrupa por PENDIENTE, APLICADA, ANULADA y luego ordena por fecha.
     *               - 'date': Ordena estrictamente por fecha de emisión descendente.
     * @return Una lista de PurchaseResponseDTO ordenadas según el criterio.
     */
    @GetMapping
    public ResponseEntity<List<PurchaseResponseDTO>> getAllPurchases(
            @RequestParam(name = "sortBy", defaultValue = "status") String sortBy) {
        if (!"status".equalsIgnoreCase(sortBy) && !"date".equalsIgnoreCase(sortBy)) {
            throw new BadRequestException("Valor de 'sortBy' no válido. Use 'status' o 'date'.");
        }
        List<PurchaseResponseDTO> purchases = purchaseService.findAll(sortBy);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Obtiene una compra específica por su ID, incluyendo sus detalles.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponseDTO> getPurchaseById(@PathVariable Integer id) {
        PurchaseResponseDTO purchase = purchaseService.findById(id);
        return ResponseEntity.ok(purchase);
    }

    /**
     * Obtiene una lista de compras de un proveedor que son elegibles para crear una nota de crédito.
     * @param supplierId El ID del proveedor.
     * @return Una lista de DTOs simplificados de las compras elegibles.
     */
    @GetMapping("/available-for-credit-note")
    public ResponseEntity<List<PurchaseForCreditNoteDTO>> getPurchasesAvailableForCreditNote(@RequestParam Integer supplierId) {
        List<PurchaseForCreditNoteDTO> purchases = purchaseService.findPurchasesAvailableForCreditNote(supplierId);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Busca compras por su estado (ej. PENDIENTE, APLICADA, ANULADA).
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseResponseDTO>> getPurchasesByStatus(@PathVariable String status) {
        List<PurchaseResponseDTO> purchases = purchaseService.findByStatus(status);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Realiza una búsqueda combinada de compras para reportes.
     * Filtra por rango de fechas y/o criterios del proveedor. Todos los parámetros son opcionales.
     */
    @GetMapping("/report")
    public ResponseEntity<List<PurchaseResponseDTO>> searchPurchasesCombined(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String supplierLastName) {
        List<PurchaseResponseDTO> purchases = purchaseService.findByCombinedCriteria(
                startDate, endDate, supplierName, supplierLastName);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Crea una nueva compra junto con sus detalles.
     */
    @PostMapping
    public ResponseEntity<PurchaseResponseDTO> createPurchase(@Valid @RequestBody PurchaseCreateDTO purchaseCreateDTO) {
        PurchaseResponseDTO createdPurchase = purchaseService.createPurchase(purchaseCreateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPurchase);
    }

    /**
     * Elimina una compra por ID. Solo permitido si la compra está en estado 'PENDIENTE'.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Integer id) {
        purchaseService.deletePurchase(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Actualiza parcialmente una compra existente. Solo permitido si está en estado 'PENDIENTE'.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PurchaseResponseDTO> updatePurchase(@PathVariable Integer id, @Valid @RequestBody PurchaseUpdateDTO updateDTO) {
        // Conectamos con el nuevo método implementado en el servicio.
        PurchaseResponseDTO updated = purchaseService.updatePurchase(id, updateDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Cambia el estado de una compra de 'PENDIENTE' a 'APLICADA'.
     */
    @PostMapping("/{id}/apply")
    @ResponseStatus(HttpStatus.OK)
    public PurchaseResponseDTO applyPurchase(@PathVariable Integer id) {
        return purchaseService.applyPurchase(id);
    }

    /**
     * Cambia el estado de una compra de 'APLICADA' a 'ANULADA'.
     */
    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.OK) // <-- CORRECCIÓN REALIZADA AQUÍ
    public PurchaseResponseDTO cancelPurchase(@PathVariable Integer id) {
        return purchaseService.cancelPurchase(id);
    }

    /**
     * Endpoint para obtener el asiento contable asociado a una compra específica.
     * Sigue el patrón de arquitectura universal para la visualización de asientos.
     * @param id El ID de la compra.
     * @return Un ResponseEntity con el DTO del asiento contable.
     */
    @GetMapping("/{id}/accounting-entry")
    public ResponseEntity<AccountingEntryResponseDTO> getPurchaseAccountingEntry(@PathVariable Integer id) {
        AccountingEntryResponseDTO entryDto = purchasesAccountingService.getEntryForPurchase(id);
        return ResponseEntity.ok(entryDto);
    }

    /**
     * Obtiene el ID de una compra específica dado su número de documento.
     * @param documentNumber El número de documento de la compra.
     * @return El ID de la compra (Integer).
     */
    @GetMapping("/document/{documentNumber}")
    public ResponseEntity<Integer> getPurchaseIdByDocumentNumber(@PathVariable String documentNumber) {
        Integer idPurchase = purchaseService.findIdByDocumentNumber(documentNumber);
        return ResponseEntity.ok(idPurchase);
    }
}