package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.TransactionAccounting;
import com.nubixconta.modules.accounting.entity.enums.AccountingTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}