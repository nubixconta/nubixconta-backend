package com.nubixconta.modules.accountsreceivable.controller;
import com.nubixconta.modules.accountsreceivable.service.AccountsReceivableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accountsreceivable")
public class AccountsReceivableController {
    @Autowired
    private AccountsReceivableService accountsReceivableService;

    // TODO: Agregar endpoints REST para cuentas por cobrar
}
