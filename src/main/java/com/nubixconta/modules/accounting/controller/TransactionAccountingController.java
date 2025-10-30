package com.nubixconta.modules.accounting.controller;

import com.nubixconta.common.exception.BadRequestException;
import com.nubixconta.modules.accounting.dto.accounting.TransactionAccountingCreateDTO;
import com.nubixconta.modules.accounting.dto.accounting.TransactionAccountingResponseDTO;
import com.nubixconta.modules.accounting.dto.accounting.TransactionAccountingUpdateDTO;
import com.nubixconta.modules.accounting.entity.enums.AccountingTransactionStatus;
import com.nubixconta.modules.accounting.service.TransactionAccountingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounting/transactions")
@RequiredArgsConstructor
public class TransactionAccountingController {

    private final TransactionAccountingService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<TransactionAccountingResponseDTO> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<TransactionAccountingResponseDTO>> getAllTransactions(
            @RequestParam(name = "sortBy", defaultValue = "status") String sortBy) {

        // Añadimos la misma validación que en Compras
        if (!"status".equalsIgnoreCase(sortBy) && !"date".equalsIgnoreCase(sortBy)) {
            throw new BadRequestException("Valor de 'sortBy' no válido. Use 'status' o 'date'.");
        }

        List<TransactionAccountingResponseDTO> transactions = transactionService.findAll(sortBy);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping
    public ResponseEntity<TransactionAccountingResponseDTO> createTransaction(@Valid @RequestBody TransactionAccountingCreateDTO createDTO) {
        TransactionAccountingResponseDTO createdTransaction = transactionService.createTransaction(createDTO);
        return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionAccountingResponseDTO> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionAccountingUpdateDTO updateDTO) {

        TransactionAccountingResponseDTO updatedTransaction = transactionService.updateTransaction(id, updateDTO);
        return ResponseEntity.ok(updatedTransaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<TransactionAccountingResponseDTO> applyTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.applyTransaction(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransactionAccountingResponseDTO> cancelTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.cancelTransaction(id));
    }

    /**
     * Realiza una búsqueda combinada de transacciones para reportes o filtros.
     * Filtra por rango de fechas y/o estado. Todos los parámetros son opcionales.
     * Replica la funcionalidad del endpoint /report de Compras.
     */
    @GetMapping("/report")
    public ResponseEntity<List<TransactionAccountingResponseDTO>> searchTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) AccountingTransactionStatus status) {

        List<TransactionAccountingResponseDTO> transactions = transactionService.findByFilters(startDate, endDate, status);
        return ResponseEntity.ok(transactions);
    }

}