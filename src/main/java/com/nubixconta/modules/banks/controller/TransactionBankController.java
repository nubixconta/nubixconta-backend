package com.nubixconta.modules.banks.controller;

import com.nubixconta.modules.banks.dto.TransactionBankCreateRequestDTO;
import com.nubixconta.modules.banks.dto.TransactionBankDTO;
import com.nubixconta.modules.banks.service.TransactionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-transactions")
@RequiredArgsConstructor
public class TransactionBankController {

    private final TransactionBankService service;



    @PutMapping("/{id}")
    public ResponseEntity<TransactionBankDTO> updateTransaction(@PathVariable Integer id, @RequestBody TransactionBankDTO dto) {
        return ResponseEntity.ok(service.updateTransaction(id, dto));
    }
    @PostMapping("/full") // O simplemente @PostMapping si este será el único de creación
    public ResponseEntity<TransactionBankDTO> createFullBankTransaction(
            @Valid @RequestBody TransactionBankCreateRequestDTO dto) {
        TransactionBankDTO createdTransaction = service.createFullTransaction(dto);
        return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
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
    public ResponseEntity<List<TransactionBankDTO>> listAll() { // <-- Sin parámetros
        List<TransactionBankDTO> results = service.listAll(); // <-- Llama al servicio sin parámetros
        return ResponseEntity.ok(results);
    }
    /**
     * Busca y filtra transacciones bancarias de forma dinámica.
     * - Si no se proporcionan parámetros, lista todas las transacciones.
     * - Permite filtrar por un término en la cuenta (nombre, código, etc.).
     * - Permite filtrar por un rango de fechas.
     *
     * @param query     Término de búsqueda opcional para la cuenta.
     * @param startDate Fecha de inicio opcional (formato YYYY-MM-DD).
     * @param endDate   Fecha de fin opcional (formato YYYY-MM-DD).
     * @return Lista de transacciones que cumplen con los criterios.
     */
    @GetMapping("/search")
    public ResponseEntity<List<TransactionBankDTO>> searchTransactions(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<TransactionBankDTO> results = service.searchTransactions(query, startDate, endDate);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionBankDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }




}
