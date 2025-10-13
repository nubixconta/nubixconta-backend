package com.nubixconta.modules.AccountsPayable.controller;

import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayablePurchaseResponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayableReponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts-payable")
public class AccountsPayableController {

    private final AccountsPayableService service;

    private final ModelMapper modelMapper;
    public AccountsPayableController(AccountsPayableService service, ModelMapper modelMapper) {
        this.service = service;
        this.modelMapper= modelMapper;
    }

    //Devuelve los datos de compras y el saldo de AccountsPayable
    @GetMapping("/purcharse-summary")
    public ResponseEntity<List<AccountsPayablePurchaseResponseDTO>> getAllAccountsPayablePurchaseSummary() {
        List<AccountsPayablePurchaseResponseDTO> results = service.findAllAccountsPayablePurchaseResponseDTO();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search-by-supplier")
    public ResponseEntity<List<Map<String, Serializable>>>  searchByCustomer(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String dui,
            @RequestParam(required = false) String nit
    ) {
        return ResponseEntity.ok(service.searchBySupplier(name, lastName, dui, nit));
    }

    //  endpoint para filtrar los datos de compras y el saldo de AccountsPayable (proveedor,numero de documento y rango de fechas)
    @GetMapping("/purcharse-summary/filtered")
    public ResponseEntity<List<AccountsPayablePurchaseResponseDTO>> getFilteredAccountsPayablePurchaseSummary(
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<AccountsPayablePurchaseResponseDTO> results = service.findFilteredAccountsPayablePurchaseResponseDTO(
                supplierName,
                documentNumber,
                startDate,
                endDate
        );
        return ResponseEntity.ok(results);
    }

    @GetMapping
    public List<AccountsPayableReponseDTO> getAll() {
        return service.findAll();
    }

    //  ENDPOINT PARA ORDENAR POR ESTADO!
    @GetMapping("/sorted-by-status")
    public ResponseEntity<List<AccountsPayableReponseDTO>> getAllSortedByStatus() {
        List<AccountsPayableReponseDTO> sortedData = service.findAllSortedByStatus();
        return ResponseEntity.ok(sortedData);
    }

    // ENDPOINT PARA FILTRAR POR FECHA!
    @GetMapping("/filter-by-date")
    public ResponseEntity<List<AccountsPayableReponseDTO>> getByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<AccountsPayableReponseDTO> filteredData = service.findByPaymentDateRange(startDate, endDate);
        return ResponseEntity.ok(filteredData);
    }
    //  ENDPOINT PARA ORDENAR POR FECHA!
    @GetMapping("/sorted-by-date")
    public ResponseEntity<List<AccountsPayableReponseDTO>> getAllSortedByDate() {
        List<AccountsPayableReponseDTO> sortedData = service.findAllSortedByDate();
        return ResponseEntity.ok(sortedData);
    }
}
