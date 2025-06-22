package com.nubixconta.modules.accounting.service;

import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }
//Filra solo las cuenta de ACTIVO-BANCO
    public List<Account> findBankAccounts() {
        return repository.findByAccountType("ACTIVO-BANCO");
    }
}
