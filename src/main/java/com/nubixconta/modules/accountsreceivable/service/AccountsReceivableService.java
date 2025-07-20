package com.nubixconta.modules.accountsreceivable.service;

import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.repository.AccountsReceivableRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccountsReceivableService {

    private final AccountsReceivableRepository repository;

    public AccountsReceivableService(AccountsReceivableRepository repository) {
        this.repository = repository;
    }

//Este metodo es para filtrar un cobro por un cliente y mostrar en una tabla el estado de cuenta por cliente
    public List<Map<String, Serializable>> searchByCustomer(String name, String lastName, String dui, String nit) {
        Specification<AccountsReceivable> spec = null;

        if (name != null && !name.isBlank()) {
            Specification<AccountsReceivable> nameSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("sale").get("customer").get("customerName")), "%" + name.toLowerCase() + "%");
            spec = (spec == null) ? nameSpec : spec.and(nameSpec);
        }

        if (lastName != null && !lastName.isBlank()) {
            Specification<AccountsReceivable> lastNameSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("sale").get("customer").get("customerLastName")), "%" + lastName.toLowerCase() + "%");
            spec = (spec == null) ? lastNameSpec : spec.and(lastNameSpec);
        }

        if (dui != null && !dui.isBlank()) {
            Specification<AccountsReceivable> duiSpec = (root, query, cb) ->
                    cb.equal(root.get("sale").get("customer").get("customerDui"), dui);
            spec = (spec == null) ? duiSpec : spec.and(duiSpec);
        }

        if (nit != null && !nit.isBlank()) {
            Specification<AccountsReceivable> nitSpec = (root, query, cb) ->
                    cb.equal(root.get("sale").get("customer").get("customerNit"), nit);
            spec = (spec == null) ? nitSpec : spec.and(nitSpec);
        }

        LocalDate today = LocalDate.now();

        return repository.findAll(spec).stream()
                .map(account -> {
                    var sale = account.getSale();
                    var customer = sale.getCustomer();
                    LocalDate issueDate = sale.getIssueDate().toLocalDate();
                    LocalDate dueDate = issueDate.plusDays(customer.getCreditDay());
                    long daysLate = today.isAfter(dueDate) ? ChronoUnit.DAYS.between(dueDate, today) : 0;

                    Map<String, Serializable> data = new HashMap<>();
                    data.put("documentNumber", sale.getDocumentNumber());
                    data.put("customerName", customer.getCustomerName());
                    data.put("customerLastName", customer.getCustomerLastName());
                    data.put("issueDate", issueDate.toString());
                    data.put("daysLate", daysLate);
                    data.put("balance", account.getBalance());

                    return data;
                })
                .collect(Collectors.toList());

    }
    public List<Map<String, Object>> findAll() {
        return repository.findAll().stream()
                .map(account -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", account.getId());
                    result.put("saleId", account.getSaleId());
                    result.put("sale", account.getSale()); // incluir el objeto completo de venta
                    result.put("balance", account.getBalance());
                    result.put("receiveAccountStatus", account.getReceiveAccountStatus());
                    result.put("receivableAccountDate", account.getReceivableAccountDate());
                    result.put("moduleType", account.getModuleType());
                    result.put("collectionDetails", account.getCollectionDetails());

                    // Agregar el creditDay del cliente
                    if (account.getSale() != null && account.getSale().getCustomer() != null) {
                        result.put("creditDay", account.getSale().getCustomer().getCreditDay());
                    } else {
                        result.put("creditDay", null);
                    }

                    return result;
                })
                .collect(Collectors.toList());
    }

    public List<AccountsReceivable> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByDateRange(start, end);
    }

    public Optional<AccountsReceivable> findById(Integer id) {
        return repository.findById(id);
    }

    public AccountsReceivable save(AccountsReceivable accountsReceivable) {
        return repository.save(accountsReceivable);
    }

    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    public AccountsReceivable update(Integer id, AccountsReceivable updated) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setSaleId(updated.getSaleId());
                    existing.setBalance(updated.getBalance());
                    existing.setReceiveAccountStatus(updated.getReceiveAccountStatus());
                    existing.setReceivableAccountDate(updated.getReceivableAccountDate());
                    existing.setModuleType(updated.getModuleType());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Cuenta por cobrar no encontrada con ID: " + id));
    }
    //Este metodo es para actualizar uno o mas campos especificos
    public AccountsReceivable partialUpdate(Integer id, Map<String, Object> updates) {
        AccountsReceivable existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta por cobrar no encontrada"));

        updates.forEach((key, value) -> {
            switch (key) {
                case "saleId" -> existing.setSaleId(Integer.parseInt(value.toString()));
                case "balance" -> existing.setBalance(new BigDecimal(value.toString()));
                case "receiveAccountStatus" -> existing.setReceiveAccountStatus((String) value);
                case "receivableAccountDate" -> existing.setReceivableAccountDate(LocalDateTime.parse(value.toString()));
                case "moduleType" -> existing.setModuleType((String) value);
            }
        });

        return repository.save(existing);
    }

}
