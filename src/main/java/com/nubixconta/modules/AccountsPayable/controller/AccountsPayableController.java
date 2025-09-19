package com.nubixconta.modules.AccountsPayable.controller;

import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayablePurchaseResponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayableReponseDTO;
import com.nubixconta.modules.AccountsPayable.service.AccountsPayableService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

}
