package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.dto.Account.AccountBankResponseDTO;
import com.nubixconta.modules.accounting.service.CollectionEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CollectionEntryService service;

    public AccountController(CollectionEntryService service) {
        this.service = service;
    }
//Metodo para filtrar solo cuentas bancarias
    @GetMapping("/bank-accounts")
    public ResponseEntity<List<AccountBankResponseDTO>> getBankAccounts() {
        return ResponseEntity.ok(service.findBankAccounts());
    }
}
