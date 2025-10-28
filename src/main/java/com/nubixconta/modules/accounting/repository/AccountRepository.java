package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    List<Account> findByAccountType(String accountType);
    Optional<Account> findByAccountNameIgnoreCase(String name);
    //para buscar la cuenta de Clientes
    // Método modificado para devolver un objeto Account completo
    @Query("SELECT a FROM Account a WHERE LOWER(a.accountName) IN ('cliente', 'clientes')")
    Optional<Account> findClientAccount();

    //para buscar la cuenta de Provedores
    // Método modificado para devolver un objeto Account completo
    @Query("SELECT a FROM Account a WHERE LOWER(a.accountName) IN ('proveedores', 'proveedores')")
    Optional<Account> findSupplierAccount();

    Optional<Account> findByGeneratedCode(String generatedCode);
}
