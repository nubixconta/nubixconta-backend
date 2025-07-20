package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }
//Metodo para filtrar solo cuentas bancarias
    @GetMapping("/bank-accounts")
    public ResponseEntity<List<Account>> getBankAccounts() {
        return ResponseEntity.ok(service.findBankAccounts());
    }
}
