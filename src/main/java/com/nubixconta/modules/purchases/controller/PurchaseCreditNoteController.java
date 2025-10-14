package com.nubixconta.modules.purchases.controller;

import com.nubixconta.common.exception.BadRequestException;
import com.nubixconta.modules.accounting.dto.AccountingEntryResponseDTO;
import com.nubixconta.modules.accounting.service.PurchasesAccountingService;
import com.nubixconta.modules.purchases.dto.creditnote.PurchaseCreditNoteCreateDTO;
import com.nubixconta.modules.purchases.dto.creditnote.PurchaseCreditNoteResponseDTO;
import com.nubixconta.modules.purchases.dto.creditnote.PurchaseCreditNoteUpdateDTO;
import com.nubixconta.modules.purchases.service.PurchaseCreditNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-credit-notes") // Endpoint base para NC de Compras
@RequiredArgsConstructor
public class PurchaseCreditNoteController {

    private final PurchaseCreditNoteService creditNoteService;
    private final PurchasesAccountingService purchasesAccountingService;

    /**
     * Obtener todas las notas de crédito de compra con ordenamiento.
     */
    @GetMapping
    public ResponseEntity<List<PurchaseCreditNoteResponseDTO>> getAllCreditNotes(
            @RequestParam(name = "sortBy", defaultValue = "status") String sortBy) {
        if (!"status".equalsIgnoreCase(sortBy) && !"date".equalsIgnoreCase(sortBy)) {
            throw new BadRequestException("Valor de 'sortBy' no válido. Use 'status' o 'date'.");
        }
        List<PurchaseCreditNoteResponseDTO> creditNotes = creditNoteService.findAll(sortBy);
        return ResponseEntity.ok(creditNotes);
    }

    /**
     * Obtener una nota de crédito de compra por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> getCreditNoteById(@PathVariable Integer id) {
        return ResponseEntity.ok(creditNoteService.findById(id));
    }

    /**
     * Crear una nueva nota de crédito de compra.
     */
    @PostMapping
    public ResponseEntity<PurchaseCreditNoteResponseDTO> createCreditNote(@Valid @RequestBody PurchaseCreditNoteCreateDTO createDTO) {
        PurchaseCreditNoteResponseDTO createdNote = creditNoteService.createCreditNote(createDTO);
        return new ResponseEntity<>(createdNote, HttpStatus.CREATED);
    }

    /**
     * Actualizar una nota de crédito de compra PENDIENTE.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> updateCreditNote(@PathVariable Integer id, @Valid @RequestBody PurchaseCreditNoteUpdateDTO updateDTO) {
        PurchaseCreditNoteResponseDTO updatedNote = creditNoteService.updateCreditNote(id, updateDTO);
        return ResponseEntity.ok(updatedNote);
    }

    /**
     * Eliminar una nota de crédito de compra PENDIENTE.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreditNote(@PathVariable Integer id) {
        creditNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Aplicar una nota de crédito de compra PENDIENTE.
     */
    @PostMapping("/{id}/apply")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> applyCreditNote(@PathVariable Integer id) {
        PurchaseCreditNoteResponseDTO appliedNote = creditNoteService.applyCreditNote(id);
        return ResponseEntity.ok(appliedNote);
    }

    /**
     * Anular una nota de crédito de compra APLICADA.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> cancelCreditNote(@PathVariable Integer id) {
        PurchaseCreditNoteResponseDTO cancelledNote = creditNoteService.cancelCreditNote(id);
        return ResponseEntity.ok(cancelledNote);
    }

    // --- PIEZA FALTANTE: Endpoint para obtener el asiento contable ---
    // Este método necesita su contraparte en el servicio de contabilidad.

    /**
     * Endpoint para obtener el asiento contable asociado a una nota de crédito de compra.
     * @param id El ID de la nota de crédito de compra.
     * @return Un ResponseEntity con el DTO del asiento contable.
     */
    @GetMapping("/{id}/accounting-entry")
    public ResponseEntity<AccountingEntryResponseDTO> getCreditNoteAccountingEntry(@PathVariable Integer id) {
        AccountingEntryResponseDTO entryDto = purchasesAccountingService.getEntryForPurchaseCreditNote(id);
        return ResponseEntity.ok(entryDto);
    }
}