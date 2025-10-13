package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.dto.BankEntryDTO;
import com.nubixconta.modules.accounting.service.BankEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-entries")
@RequiredArgsConstructor
public class BankEntryController {

    private final BankEntryService service;

    @PostMapping
    public ResponseEntity<BankEntryDTO> createEntry(@RequestBody BankEntryDTO dto) {
        return ResponseEntity.ok(service.createEntry(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BankEntryDTO> updateEntry(@PathVariable Integer id, @RequestBody BankEntryDTO dto) {
        return ResponseEntity.ok(service.updateEntry(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Integer id) {
        service.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<BankEntryDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
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
