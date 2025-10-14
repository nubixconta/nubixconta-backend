package com.nubixconta.modules.purchases.controller;

import com.nubixconta.common.exception.BadRequestException;
import com.nubixconta.modules.purchases.dto.creditnote.PurchaseCreditNoteCreateDTO;
import com.nubixconta.modules.purchases.dto.creditnote.PurchaseCreditNoteResponseDTO;
import com.nubixconta.modules.purchases.dto.creditnote.PurchaseCreditNoteUpdateDTO;
import com.nubixconta.modules.purchases.service.PurchaseCreditNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-credit-notes")
@RequiredArgsConstructor
public class PurchaseCreditNoteController {

    private final PurchaseCreditNoteService creditNoteService;

    @GetMapping
    public ResponseEntity<List<PurchaseCreditNoteResponseDTO>> getAll(
            @RequestParam(name = "sortBy", defaultValue = "status") String sortBy) {
        if (!"status".equalsIgnoreCase(sortBy) && !"date".equalsIgnoreCase(sortBy)) {
            throw new BadRequestException("Valor de 'sortBy' no válido. Use 'status' o 'date'.");
        }
        return ResponseEntity.ok(creditNoteService.findAll(sortBy));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(creditNoteService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseCreditNoteResponseDTO> create(@Valid @RequestBody PurchaseCreditNoteCreateDTO createDTO) {
        PurchaseCreditNoteResponseDTO createdNote = creditNoteService.createCreditNote(createDTO);
        return new ResponseEntity<>(createdNote, HttpStatus.CREATED);
    }

    // Aquí irían los endpoints para PATCH y DELETE

    @PostMapping("/{id}/apply")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> apply(@PathVariable Integer id) {
        PurchaseCreditNoteResponseDTO appliedNote = creditNoteService.applyCreditNote(id);
        return ResponseEntity.ok(appliedNote);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseCreditNoteResponseDTO> cancel(@PathVariable Integer id) {
        // Suponiendo que el método cancelCreditNote existe en el servicio
        // PurchaseCreditNoteResponseDTO cancelledNote = creditNoteService.cancelCreditNote(id);
        // return ResponseEntity.ok(cancelledNote);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build(); // Placeholder
    }

    // Aquí irían los endpoints de búsqueda (by-purchase, search, etc.)
}