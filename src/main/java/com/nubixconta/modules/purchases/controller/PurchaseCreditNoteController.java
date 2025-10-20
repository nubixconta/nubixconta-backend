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
@RequestMapping("/api/v1/purchase-credit-notes")
@RequiredArgsConstructor
public class PurchaseCreditNoteController {

    private final PurchaseCreditNoteService creditNoteService;
    private final PurchasesAccountingService purchasesAccountingService;

    @GetMapping
    public ResponseEntity<List<PurchaseCreditNoteResponseDTO>> getAllCreditNotes(
            @RequestParam(name = "sortBy", defaultValue = "status") String sortBy) {
        if (!"status".equalsIgnoreCase(sortBy) && !"date".equalsIgnoreCase(sortBy)) {
            throw new BadRequestException("Valor de 'sortBy' no válido. Use 'status' o 'date'.");
        }
        List<PurchaseCreditNoteResponseDTO> creditNotes = creditNoteService.findAll(sortBy);
        return ResponseEntity.ok(creditNotes);
    }

    @GetMapping("/{idPurchaseCreditNote}")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> getCreditNoteById(@PathVariable Integer idPurchaseCreditNote) {
        return ResponseEntity.ok(creditNoteService.findById(idPurchaseCreditNote));
    }

    @GetMapping("/by-purchase/{purchaseId}")
    public ResponseEntity<List<PurchaseCreditNoteResponseDTO>> getCreditNotesByPurchase(@PathVariable Integer purchaseId) {
        return ResponseEntity.ok(creditNoteService.findByPurchaseId(purchaseId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PurchaseCreditNoteResponseDTO>> searchByDateAndStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(creditNoteService.findByDateRangeAndStatus(start, end, status));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<PurchaseCreditNoteResponseDTO>> getByStatus(@RequestParam String status) {
        return ResponseEntity.ok(creditNoteService.findByStatus(status));
    }

    @PostMapping
    public ResponseEntity<PurchaseCreditNoteResponseDTO> createCreditNote(@Valid @RequestBody PurchaseCreditNoteCreateDTO createDTO) {
        PurchaseCreditNoteResponseDTO createdNote = creditNoteService.createCreditNote(createDTO);
        return new ResponseEntity<>(createdNote, HttpStatus.CREATED);
    }

    @PatchMapping("/{idPurchaseCreditNote}")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> updateCreditNote(@PathVariable Integer idPurchaseCreditNote, @Valid @RequestBody PurchaseCreditNoteUpdateDTO updateDTO) { // <-- ¡CORREGIDO!
        PurchaseCreditNoteResponseDTO updatedNote = creditNoteService.updateCreditNote(idPurchaseCreditNote, updateDTO);
        return ResponseEntity.ok(updatedNote);
    }

    @DeleteMapping("/{idPurchaseCreditNote}")
    public ResponseEntity<Void> deleteCreditNote(@PathVariable Integer idPurchaseCreditNote) { // <-- ¡CORREGIDO!
        creditNoteService.delete(idPurchaseCreditNote);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{idPurchaseCreditNote}/apply")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> applyCreditNote(@PathVariable Integer idPurchaseCreditNote) { // <-- ¡CORREGIDO!
        PurchaseCreditNoteResponseDTO appliedNote = creditNoteService.applyCreditNote(idPurchaseCreditNote);
        return ResponseEntity.ok(appliedNote);
    }

    @PostMapping("/{idPurchaseCreditNote}/cancel")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> cancelCreditNote(@PathVariable Integer idPurchaseCreditNote) { // <-- ¡CORREGIDO!
        PurchaseCreditNoteResponseDTO cancelledNote = creditNoteService.cancelCreditNote(idPurchaseCreditNote);
        return ResponseEntity.ok(cancelledNote);
    }

    @GetMapping("/{idPurchaseCreditNote}/accounting-entry")
    public ResponseEntity<AccountingEntryResponseDTO> getCreditNoteAccountingEntry(@PathVariable Integer idPurchaseCreditNote) { // <-- ¡CORREGIDO!
        AccountingEntryResponseDTO entryDto = purchasesAccountingService.getEntryForPurchaseCreditNote(idPurchaseCreditNote);
        return ResponseEntity.ok(entryDto);
    }
}