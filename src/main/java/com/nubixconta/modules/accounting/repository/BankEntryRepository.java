package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.BankEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankEntryRepository extends JpaRepository<BankEntry, Integer> {

    // Buscar todas las entradas contables asociadas a una transacción bancaria
    List<BankEntry> findByTransactionBank_IdBankTransaction(Integer idBankTransaction);
}
