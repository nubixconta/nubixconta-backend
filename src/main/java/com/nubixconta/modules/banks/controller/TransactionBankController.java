package com.nubixconta.modules.banks.controller;

import com.nubixconta.modules.banks.dto.TransactionBankDTO;
import com.nubixconta.modules.banks.service.TransactionBankService;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-transactions")
@RequiredArgsConstructor
public class TransactionBankController {

    private final TransactionBankService service;

    @PostMapping
    public ResponseEntity<TransactionBankDTO> createTransaction(@RequestBody TransactionBankDTO dto) {
        return ResponseEntity.ok(service.createTransaction(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionBankDTO> updateTransaction(@PathVariable Integer id, @RequestBody TransactionBankDTO dto) {
        return ResponseEntity.ok(service.updateTransaction(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Integer id) {
        service.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<TransactionBankDTO> applyTransaction(@PathVariable Integer id) {
        return ResponseEntity.ok(service.applyTransaction(id));
    }

    @PostMapping("/{id}/annul")
    public ResponseEntity<TransactionBankDTO> annulTransaction(@PathVariable Integer id) {
        return ResponseEntity.ok(service.annulTransaction(id));
    }

    @GetMapping
    public ResponseEntity<List<TransactionBankDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionBankDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
