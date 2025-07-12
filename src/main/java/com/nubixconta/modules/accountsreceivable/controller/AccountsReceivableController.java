package com.nubixconta.modules.accountsreceivable.controller;

import com.nubixconta.modules.accountsreceivable.dto.accountsreceivable.AccountsReceivableResponseDTO;
import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.service.AccountsReceivableService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.Serializable;
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
    //  buscar accountReceivable por saleId
    @GetMapping("/by-sale/{saleId}")
    public ResponseEntity<AccountsReceivable> getBySaleId(@PathVariable Integer saleId) {
        return service.findBySaleId(saleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
