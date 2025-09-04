package com.nubixconta.modules.accountsreceivable.controller;

import com.nubixconta.modules.accountsreceivable.dto.accountsreceivable.AccountsReceivableResponseDTO;
import com.nubixconta.modules.accountsreceivable.dto.accountsreceivable.AccountsReceivableSaleResponseDTO;
import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.service.AccountsReceivableService;
import org.modelmapper.ModelMapper;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final ModelMapper modelMapper;

    public AccountsReceivableController(AccountsReceivableService service,ModelMapper modelMapper) {
        this.service = service;
        this.modelMapper= modelMapper;
    }

    @GetMapping
    public List<AccountsReceivableResponseDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/sales-summary")
    public ResponseEntity<List<AccountsReceivableSaleResponseDTO>> getAllAccountsReceivableSalesSummary() {
        List<AccountsReceivableSaleResponseDTO> results = service.findAllAccountsReceivableSaleResponseDTO();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<AccountsReceivable> getById(@PathVariable Integer id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
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
    // Nuevo endpoint para buscar Cuentas por Cobrar por rango de fechas de la Venta (issueDate)


    //  buscar accountReceivable por saleId
    @GetMapping("/by-sale/{saleId}")
    public ResponseEntity<AccountsReceivable> getBySaleId(@PathVariable Integer saleId) {
        return service.findBySaleId(saleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    // ENDPOINT PARA FILTRAR POR FECHA!
    @GetMapping("/filter-by-date")
    public ResponseEntity<List<AccountsReceivableResponseDTO>> getByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<AccountsReceivableResponseDTO> filteredData = service.findByCollectionDateRange(startDate, endDate);
        return ResponseEntity.ok(filteredData);
    }
    //  ENDPOINT PARA ORDENAR POR FECHA!
    @GetMapping("/sorted-by-date")
    public ResponseEntity<List<AccountsReceivableResponseDTO>> getAllSortedByDate() {
        List<AccountsReceivableResponseDTO> sortedData = service.findAllSortedByDate();
        return ResponseEntity.ok(sortedData);
    }
    //  ENDPOINT PARA ORDENAR POR ESTADO!
    @GetMapping("/sorted-by-status")
    public ResponseEntity<List<AccountsReceivableResponseDTO>> getAllSortedByStatus() {
        List<AccountsReceivableResponseDTO> sortedData = service.findAllSortedByStatus();
        return ResponseEntity.ok(sortedData);
    }

}
