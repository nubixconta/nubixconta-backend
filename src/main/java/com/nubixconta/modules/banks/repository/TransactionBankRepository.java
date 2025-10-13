package com.nubixconta.modules.banks.repository;

import com.nubixconta.modules.banks.entity.TransactionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionBankRepository extends JpaRepository<TransactionBank, Integer> {

    List<TransactionBank> findByAccountingTransactionStatus(String status);

    List<TransactionBank> findByModuleType(String moduleType);
}
