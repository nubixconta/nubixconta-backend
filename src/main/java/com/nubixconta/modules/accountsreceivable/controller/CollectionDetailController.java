package com.nubixconta.modules.accountsreceivable.controller;

import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.repository.AccountsReceivableRepository;
import com.nubixconta.modules.accountsreceivable.service.CollectionDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/collection-detail")
public class CollectionDetailController {

    private final CollectionDetailService service;
    private final AccountsReceivableRepository accountsReceivableRepository;

    public CollectionDetailController(CollectionDetailService service, AccountsReceivableRepository accountsReceivableRepository) {
        this.service = service;
        this.accountsReceivableRepository = accountsReceivableRepository;
    }

    @GetMapping
    public List<CollectionDetail> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionDetail> getById(@PathVariable Integer id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }




    @PatchMapping("/{id}")
    public ResponseEntity<CollectionDetail> partialUpdate(@PathVariable Integer id, @RequestBody CollectionDetail partial) {
        return service.findById(id)
                .map(existing -> {
                    if (partial.getAccountReceivable() != null && partial.getAccountReceivable().getId() != null) {
                        var accountReceivable = accountsReceivableRepository.findById(partial.getAccountReceivable().getId())
                                .orElseThrow(() -> new RuntimeException("No existe accountReceivable con ID: " + partial.getAccountReceivable().getId()));
                        existing.setAccountReceivable(accountReceivable);
                    }
                    if (partial.getAccountId() != null) existing.setAccountId(partial.getAccountId());
                    if (partial.getReference() != null) existing.setReference(partial.getReference());
                    if (partial.getPaymentMethod() != null) existing.setPaymentMethod(partial.getPaymentMethod());
                    if (partial.getPaymentStatus() != null) existing.setPaymentStatus(partial.getPaymentStatus());
                    if (partial.getPaymentAmount() != null) existing.setPaymentAmount(partial.getPaymentAmount());
                    if (partial.getPaymentDetailDescription() != null) existing.setPaymentDetailDescription(partial.getPaymentDetailDescription());
                    if (partial.getModuleType() != null) existing.setModuleType(partial.getModuleType());
                    return ResponseEntity.ok(service.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    //Busca cobros por un rango de fechas
    @GetMapping("/search-by-date")
    public ResponseEntity<List<CollectionDetail>> searchByDateRange(
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Se convierte la fecha al inicio y fin del día
        LocalDate startDate = LocalDate.parse(startStr, formatter);
        LocalDateTime start = startDate.atStartOfDay();

        LocalDate endDate = LocalDate.parse(endStr, formatter);
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<CollectionDetail> results = service.findByDateRange(start, end);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/register-payment")
    public ResponseEntity<CollectionDetail> registerPayment(@RequestBody CollectionDetail detail) {
        return ResponseEntity.ok(service.registerPayment(detail));
    }
    @PostMapping
    public ResponseEntity<CollectionDetail> create(@RequestBody CollectionDetail detail) {
        return ResponseEntity.ok(service.save(detail));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
