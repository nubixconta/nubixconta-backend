package com.nubixconta.modules.accountsreceivable.service;
import com.nubixconta.modules.accountsreceivable.repository.AccountsReceivableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountsReceivableService {

    @Autowired
    private AccountsReceivableRepository accountsReceivableRepository;

    // TODO: Agregar lógica de negocio para cuentas por cobrar
}
