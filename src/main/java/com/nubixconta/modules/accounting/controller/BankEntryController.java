package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.dto.BankEntryDTO;
import com.nubixconta.modules.accounting.dto.CollectionEntry.CollectionEntryFronBankResponseDTO;
import com.nubixconta.modules.accounting.dto.bank.BankEntryResponseDTO;
import com.nubixconta.modules.accounting.service.BankEntryService;
import com.nubixconta.modules.accounting.service.CollectionEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bank-entries")
@RequiredArgsConstructor
public class BankEntryController {

    private final BankEntryService service;
    private final CollectionEntryService collectionEntryService;

    /**
     * Obtiene todos los asientos de banco, con opción de filtrar por accountName.
     * Ejemplo de uso:
     * - Para obtener todos: GET /api/v1/bank-entries
     * - Para filtrar:      GET /api/v1/bank-entries?accountName=Banco Cuscatlan
     */
    @GetMapping
    public ResponseEntity<List<CollectionEntryFronBankResponseDTO>> getAllBankEntries(
            @RequestParam(name = "accountName", required = false) String accountName,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 1. Obtener todas las entradas (sin cambios)
        List<CollectionEntryFronBankResponseDTO> combinedEntries = new ArrayList<>();
        combinedEntries.addAll(collectionEntryService.getAllCollectionEntriesForBank());
        combinedEntries.addAll(collectionEntryService.getAllPaymentEntriesForBank());

        // 2. Aplicar filtros de forma encadenada
        List<CollectionEntryFronBankResponseDTO> filteredEntries = combinedEntries.stream()
                // Filtro por nombre de cuenta (si se proporciona)
                .filter(entry -> accountName == null || accountName.trim().isEmpty() || accountName.equalsIgnoreCase(entry.getAccountName()))

                // Filtro por fecha de inicio (si se proporciona)
                // La fecha del entry no debe ser anterior a la startDate
                .filter(entry -> startDate == null || !entry.getDate().toLocalDate().isBefore(startDate))

                // Filtro por fecha de fin (si se proporciona)
                // La fecha del entry no debe ser posterior a la endDate
                .filter(entry -> endDate == null || !entry.getDate().toLocalDate().isAfter(endDate))

                .collect(Collectors.toList());

        // 3. Devolver la respuesta
        if (filteredEntries.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(filteredEntries);
    }



  /*  @PutMapping("/{id}")
    public ResponseEntity<BankEntryDTO> updateEntry(@PathVariable Integer id, @RequestBody BankEntryDTO dto) {
        return ResponseEntity.ok(service.updateEntry(id, dto));
    }*/

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Integer id) {
        service.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bank")
    public ResponseEntity<List<BankEntryResponseDTO>> listAll(
            @RequestParam(name = "accountName", required = false) String accountName,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<BankEntryResponseDTO> entries = service.listAll(accountName, startDate, endDate);

        if (entries.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(entries);
    }


    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<BankEntryDTO>> listByTransaction(@PathVariable Integer transactionId) {
        return ResponseEntity.ok(service.listByTransaction(transactionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankEntryDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
