package com.nubixconta.modules.accountsreceivable.controller;

import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.service.AccountsReceivableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts-receivable")
public class AccountsReceivableController {

    private final AccountsReceivableService service;

    public AccountsReceivableController(AccountsReceivableService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return service.findAll();
    }


    @GetMapping("/id/{id}")
    public ResponseEntity<AccountsReceivable> getById(@PathVariable Integer id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AccountsReceivable> create(@RequestBody AccountsReceivable accountsReceivable) {
        return ResponseEntity.ok(service.save(accountsReceivable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountsReceivable> update(@PathVariable Integer id, @RequestBody AccountsReceivable updated) {
        try {
            return ResponseEntity.ok(service.update(id, updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // Se usara cuando se quiera actualizar un campo en especifico como el estado o el saldo
    @PatchMapping("/{id}")
    public ResponseEntity<AccountsReceivable> partialUpdate(@PathVariable Integer id, @RequestBody Map<String, Object> updates) {
        try {
            return ResponseEntity.ok(service.partialUpdate(id, updates));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    //Busca cobros por un rango de fechas
    @GetMapping("/search-by-date")
    public ResponseEntity<List<AccountsReceivable>> searchByDateRange(
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Se convierte la fecha al inicio y fin del día
        LocalDate startDate = LocalDate.parse(startStr, formatter);
        LocalDateTime start = startDate.atStartOfDay();

        LocalDate endDate = LocalDate.parse(endStr, formatter);
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<AccountsReceivable> results = service.findByDateRange(start, end);
        return ResponseEntity.ok(results);
    }
    @GetMapping("/search-by-customer")
    public ResponseEntity<List<Map<String, Serializable>>>  searchByCustomer(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String dui,
            @RequestParam(required = false) String nit
    ) {
        return ResponseEntity.ok(service.searchByCustomer(name, lastName, dui, nit));
    }

}
