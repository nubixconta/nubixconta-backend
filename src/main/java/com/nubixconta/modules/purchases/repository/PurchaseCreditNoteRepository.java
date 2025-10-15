package com.nubixconta.modules.purchases.repository;

import com.nubixconta.modules.purchases.entity.PurchaseCreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository // <-- Es buena práctica añadir esta anotación
public interface PurchaseCreditNoteRepository extends JpaRepository<PurchaseCreditNote, Integer> {

    // --- MÉTODOS DE VALIDACIÓN (sin cambios) ---
    boolean existsByCompany_IdAndDocumentNumber(Integer companyId, String documentNumber);
    boolean existsByCompany_IdAndPurchase_IdPurchaseAndCreditNoteStatusIn(Integer companyId, Integer purchaseId, List<String> statuses);


    // --- MÉTODO OPTIMIZADO PARA findById (este está correcto y se mantiene) ---
    @Query("SELECT pcn FROM PurchaseCreditNote pcn " +
            "LEFT JOIN FETCH pcn.purchase p " +
            "LEFT JOIN FETCH p.supplier " +
            "LEFT JOIN FETCH pcn.details d " +
            "LEFT JOIN FETCH d.product " +
            "LEFT JOIN FETCH d.catalog c " +
            "LEFT JOIN FETCH c.account " +
            "WHERE pcn.idPurchaseCreditNote = :idPurchaseCreditNote")
    Optional<PurchaseCreditNote> findByIdWithDetails(@Param("idPurchaseCreditNote") Integer id);


    // --- MÉTODO OPTIMIZADO PARA findAll (ordenado por estado) - CORREGIDO ---
    // Se ha eliminado la palabra clave 'DISTINCT'
    @Query("SELECT pcn FROM PurchaseCreditNote pcn " +
            "LEFT JOIN FETCH pcn.purchase p " +
            "LEFT JOIN FETCH p.supplier " +
            "WHERE pcn.company.id = :companyId " +
            "ORDER BY CASE pcn.creditNoteStatus WHEN 'PENDIENTE' THEN 1 WHEN 'APLICADA' THEN 2 WHEN 'ANULADA' THEN 3 ELSE 4 END, pcn.issueDate DESC")
    List<PurchaseCreditNote> findAllWithDetailsByCompanyIdOrderByStatus(@Param("companyId") Integer companyId);

    // --- MÉTODO OPTIMIZADO PARA findAll (ordenado por fecha) - CORREGIDO ---
    // Se ha eliminado la palabra clave 'DISTINCT'
    @Query("SELECT pcn FROM PurchaseCreditNote pcn " +
            "LEFT JOIN FETCH pcn.purchase p " +
            "LEFT JOIN FETCH p.supplier " +
            "WHERE pcn.company.id = :companyId " +
            "ORDER BY pcn.issueDate DESC")
    List<PurchaseCreditNote> findAllWithDetailsByCompanyIdOrderByDate(@Param("companyId") Integer companyId);


    // --- MÉTODOS DE BÚSQUEDA PARA ENDPOINTS ESPECÍFICOS (se mantienen) ---
    // Idealmente, también deberían optimizarse con JOIN FETCH si se usan para vistas de detalle.
    List<PurchaseCreditNote> findByCompany_IdAndPurchase_IdPurchase(Integer companyId, Integer purchaseId);
    List<PurchaseCreditNote> findByCompany_IdAndCreditNoteStatus(Integer companyId, String status);

    @Query("SELECT n FROM PurchaseCreditNote n WHERE n.company.id = :companyId " +
            "AND n.issueDate >= :start AND n.issueDate <= :end " +
            "AND (:status IS NULL OR n.creditNoteStatus = :status)")
    List<PurchaseCreditNote> findByCompanyIdAndDateRangeAndStatus(
            @Param("companyId") Integer companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") String status
    );
}