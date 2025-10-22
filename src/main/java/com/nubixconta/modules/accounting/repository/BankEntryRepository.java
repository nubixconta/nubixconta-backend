package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.BankEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BankEntryRepository extends JpaRepository<BankEntry, Integer> {

    // Buscar todas las entradas contables asociadas a una transacción bancaria
    List<BankEntry> findByTransactionBank_IdBankTransaction(Integer idBankTransaction);

    @Query("SELECT SUM(e.debit) FROM BankEntry e WHERE e.transactionBank.idBankTransaction = :transactionId")
    BigDecimal sumDebitsByTransactionId(@Param("transactionId") Integer transactionId);

    @Query("SELECT SUM(e.credit) FROM BankEntry e WHERE e.transactionBank.idBankTransaction = :transactionId")
    BigDecimal sumCreditsByTransactionId(@Param("transactionId") Integer transactionId);

    List<BankEntry> findByTransactionBank_AccountingTransactionStatusOrderByDateDesc(String status);

    @Query("SELECT e FROM BankEntry e WHERE e.transactionBank.idBankTransaction = :transactionId AND e.idCatalog = :catalogId AND e.date BETWEEN :start AND :end ORDER BY e.date DESC")
    List<BankEntry> filterEntries(@Param("transactionId") Integer transactionId,
                                @Param("catalogId") Integer catalogId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}
