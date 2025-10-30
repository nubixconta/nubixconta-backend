package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.TransactionAccounting;
import com.nubixconta.modules.accounting.entity.enums.AccountingTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionAccountingRepository extends JpaRepository<TransactionAccounting, Long> {

    /**
     * Busca una transacción contable por su ID, asegurando que pertenezca a la empresa actual.
     * Es el método principal para obtener una entidad de forma segura.
     * @param id El ID de la transacción contable.
     * @param companyId El ID de la empresa del contexto de seguridad.
     * @return Un Optional que contiene la transacción si se encuentra.
     */
    Optional<TransactionAccounting> findByIdAndCompanyId(Long id, Integer companyId);

    /**
     * Devuelve todas las transacciones contables para la empresa actual, ordenadas por fecha descendente.
     * @param companyId El ID de la empresa.
     * @return Una lista de transacciones contables.
     */
    List<TransactionAccounting> findAllByCompanyIdOrderByTransactionDateDesc(Integer companyId);

    /**
     * Devuelve todas las transacciones contables para la empresa actual que coincidan con un estado específico.
     * @param companyId El ID de la empresa.
     * @param status El estado por el cual filtrar (PENDIENTE, APLICADA, ANULADA).
     * @return Una lista de transacciones contables filtradas.
     */
    List<TransactionAccounting> findAllByCompanyIdAndStatusOrderByTransactionDateDesc(Integer companyId, AccountingTransactionStatus status);

    /**
     * Busca transacciones contables aplicando filtros opcionales de rango de fechas y estado.
     * Sigue el patrón del repositorio de Compras.
     * @param companyId El ID de la empresa.
     * @param startDate La fecha de inicio del rango (opcional).
     * @param endDate La fecha de fin del rango (opcional).
     * @param status El estado por el cual filtrar (opcional).
     * @return Una lista de transacciones que coinciden con los filtros.
     */
    @Query("SELECT t FROM TransactionAccounting t WHERE t.company.id = :companyId " +
            "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "ORDER BY t.transactionDate DESC")
    List<TransactionAccounting> findByFilters(
            @Param("companyId") Integer companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") AccountingTransactionStatus status
    );

    /**
     * Devuelve todas las transacciones ordenadas por estado y luego por fecha.
     * VERSIÓN FINAL CORREGIDA: Usa la ruta del paquete correcta para el Enum.
     */
    @Query("SELECT t FROM TransactionAccounting t WHERE t.company.id = :companyId ORDER BY " +
            "CASE t.status " +
            // --- ESTA ES LA RUTA CORREGIDA BASADA EN TU ESTRUCTURA ---
            "  WHEN com.nubixconta.modules.accounting.entity.enums.AccountingTransactionStatus.PENDIENTE THEN 1 " +
            "  WHEN com.nubixconta.modules.accounting.entity.enums.AccountingTransactionStatus.APLICADA  THEN 2 " +
            "  WHEN com.nubixconta.modules.accounting.entity.enums.AccountingTransactionStatus.ANULADA   THEN 3 " +
            "  ELSE 4 " +
            "END, " +
            "t.transactionDate DESC")
    List<TransactionAccounting> findAllByCompanyIdOrderByStatusAndDate(@Param("companyId") Integer companyId);
}