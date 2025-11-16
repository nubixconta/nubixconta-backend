package com.nubixconta.modules.banks.repository;

import com.nubixconta.modules.banks.entity.TransactionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
     * Busca transacciones de forma dinámica.
     * Los filtros se aplican solo si los parámetros no son nulos.
     *
     * @param query     Término de búsqueda opcional para nombre, código o nombre personalizado de la cuenta.
     * @param startDate Fecha de inicio opcional para el rango de búsqueda.
     * @param endDate   Fecha de fin opcional para el rango de búsqueda.
     * @return Lista de transacciones que coinciden con los filtros aplicados.
     */
    @Query("SELECT DISTINCT tb FROM TransactionBank tb " +
            "LEFT JOIN tb.bankEntries be " + // Usamos LEFT JOIN por si una transacción no tiene asientos aún
            "LEFT JOIN be.idCatalog c " +
            "LEFT JOIN c.account a " +
            "WHERE " +
            // Condición para el término de búsqueda (se ignora si es nulo o vacío)
            "(:query IS NULL OR :query = '' OR " +
            "    LOWER(a.accountName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "    LOWER(a.generatedCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "    LOWER(c.customName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND " +
            // Condición para la fecha de inicio (se ignora si es nula)
            "(:startDate IS NULL OR tb.transactionDate >= :startDate) " +
            "AND " +
            // Condición para la fecha de fin (se ignora si es nula)
            "(:endDate IS NULL OR tb.transactionDate <= :endDate)")
    List<TransactionBank> searchTransactionsDynamically(
            @Param("query") String query,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
