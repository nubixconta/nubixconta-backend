package com.nubixconta.modules.sales.controller;

import com.nubixconta.modules.sales.dto.creditnote.CreditNoteCreateDTO;
import com.nubixconta.modules.sales.dto.creditnote.CreditNoteResponseDTO;
import com.nubixconta.modules.sales.dto.creditnote.CreditNoteUpdateDTO;
import com.nubixconta.modules.sales.service.CreditNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/credit-notes")
@RequiredArgsConstructor
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    @GetMapping
    public ResponseEntity<List<CreditNoteResponseDTO>> getAllCreditNotes() {
        return ResponseEntity.ok(creditNoteService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditNoteResponseDTO> getCreditNoteById(@PathVariable Integer id) {
        return ResponseEntity.ok(creditNoteService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CreditNoteResponseDTO> createCreditNote(@Valid @RequestBody CreditNoteCreateDTO createDTO) {
        CreditNoteResponseDTO createdNote = creditNoteService.createCreditNote(createDTO);
        return new ResponseEntity<>(createdNote, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CreditNoteResponseDTO> updateCreditNote(@PathVariable Integer id, @Valid @RequestBody CreditNoteUpdateDTO updateDTO) {
        CreditNoteResponseDTO updatedNote = creditNoteService.updateCreditNote(id, updateDTO);
        return ResponseEntity.ok(updatedNote);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreditNote(@PathVariable Integer id) {
        creditNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-sale/{saleId}")
    public ResponseEntity<List<CreditNoteResponseDTO>> getCreditNotesBySale(@PathVariable Integer saleId) {
        return ResponseEntity.ok(creditNoteService.findBySaleId(saleId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CreditNoteResponseDTO>> searchByDateAndStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(creditNoteService.findByDateRangeAndStatus(start, end, status));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<CreditNoteResponseDTO>> getByStatus(@RequestParam String status) {
        return ResponseEntity.ok(creditNoteService.findByStatus(status));
    }
}