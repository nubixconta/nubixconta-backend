package com.nubixconta.modules.banks.repository;

import com.nubixconta.modules.banks.entity.TransactionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionBankRepository extends JpaRepository<TransactionBank, Integer>, 
                                                    JpaSpecificationExecutor<TransactionBank> {

    List<TransactionBank> findByAccountingTransactionStatus(String status);

    List<TransactionBank> findByModuleType(String moduleType);

    List<TransactionBank> findByAccountingTransactionStatusOrderByTransactionDateDesc(String status);

    @Query("SELECT t FROM TransactionBank t WHERE t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<TransactionBank> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    boolean existsByReceiptNumber(String receiptNumber);

    Optional<TransactionBank> findByReceiptNumber(String receiptNumber);

    /**
     * Busca transacciones bancarias por un término de búsqueda en el nombre de la cuenta
     * o en el código generado de la cuenta asociada a la BankEntry.
     * El término de búsqueda es case-insensitive.
     *
     * @param searchTerm El término a buscar en accountName o generatedCode.
     * @return Una lista de TransactionBank que cumplen con el criterio.
     */
    @Query("SELECT DISTINCT tb FROM TransactionBank tb " +
            "JOIN tb.bankEntries be " +
            "JOIN be.idCatalog c " +
            "JOIN c.account a " +
            "WHERE LOWER(a.accountName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(a.generatedCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<TransactionBank> findByAccountNameOrCodeContainingIgnoreCase(@Param("searchTerm") String searchTerm);
}
