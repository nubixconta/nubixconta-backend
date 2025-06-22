package com.nubixconta.modules.sales.controller;

import com.nubixconta.modules.sales.entity.CreditNote;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.service.CreditNoteService;
import com.nubixconta.modules.sales.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/credit-notes")
@RequiredArgsConstructor
public class CreditNoteController {

    private final CreditNoteService creditNoteService;
    private final SaleService saleService;

    // Obtener todas las notas de crédito
    @GetMapping
    public List<CreditNote> getAllCreditNotes() {
        return creditNoteService.findAll();
    }

    // Obtener una nota de crédito por su ID
    @GetMapping("/{id}")
    public ResponseEntity<CreditNote> getCreditNote(@PathVariable Integer id) {
        return creditNoteService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Obtener todas las notas de crédito asociadas a una venta
    @GetMapping("/by-sale/{saleId}")
    public ResponseEntity<List<CreditNote>> getCreditNotesBySale(@PathVariable Integer saleId) {
        List<CreditNote> notes = creditNoteService.findBySaleId(saleId);
        if (notes.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(notes);
    }

    // Crear una nueva nota de crédito (la venta asociada debe existir)
    @PostMapping
    public ResponseEntity<CreditNote> createCreditNote(@Valid @RequestBody CreditNote creditNote) {
        Integer saleId = creditNote.getSale() != null ? creditNote.getSale().getSaleId() : null;
        if (saleId == null) {
            return ResponseEntity.badRequest().body(null);
        }
        Optional<Sale> saleOpt = saleService.findById(saleId);
        if (saleOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        creditNote.setSale(saleOpt.get());
        return ResponseEntity.ok(creditNoteService.save(creditNote));
    }

    // Actualizar completamente una nota de crédito
    @PatchMapping("/{id}")
    public ResponseEntity<CreditNote> patchCreditNote(@PathVariable Integer id, @RequestBody CreditNote updates) {
        Optional<CreditNote> optionalNote = creditNoteService.findById(id);
        if (optionalNote.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CreditNote creditNote = optionalNote.get();

        // Solo actualiza los campos enviados (campos nulos se ignoran)
        if (updates.getDocumentNumber() != null) creditNote.setDocumentNumber(updates.getDocumentNumber());
        if (updates.getCreditNoteStatus() != null) creditNote.setCreditNoteStatus(updates.getCreditNoteStatus());
        if (updates.getCreditNoteDate() != null) creditNote.setCreditNoteDate(updates.getCreditNoteDate());
        if (updates.getSale() != null && updates.getSale().getSaleId() != null) {
            Optional<Sale> saleOpt = saleService.findById(updates.getSale().getSaleId());
            saleOpt.ifPresent(creditNote::setSale);
        }

        return ResponseEntity.ok(creditNoteService.save(creditNote));
    }

    // Eliminar una nota de crédito
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreditNote(@PathVariable Integer id) {
        try {
            creditNoteService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}