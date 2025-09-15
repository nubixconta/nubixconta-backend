package com.nubixconta.modules.sales.repository;

import com.nubixconta.modules.sales.entity.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Integer> {

    // =========================================================================================
    // == INICIO DE CÓDIGO MODIFICADO: Consultas "Tenant-Aware"
    // =========================================================================================

    // Comprueba si un número de documento ya existe DENTRO de una empresa específica.
    boolean existsByCompany_IdAndDocumentNumber(Integer companyId, String documentNumber);

    // Búsquedas acotadas a la empresa.
    List<CreditNote> findByCompany_IdAndSale_SaleId(Integer companyId, Integer saleId);
    List<CreditNote> findByCompany_IdAndCreditNoteStatus(Integer companyId, String status);

    // La búsqueda por rango de fechas y estado ahora también requiere el companyId.
    @Query("SELECT n FROM CreditNote n WHERE n.company.id = :companyId " +
            "AND n.issueDate >= :start AND n.issueDate <= :end " +
            "AND (:status IS NULL OR n.creditNoteStatus = :status)")
    List<CreditNote> findByCompanyIdAndDateRangeAndStatus(
            @Param("companyId") Integer companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") String status
    );

    // La verificación de existencia ahora también debe estar acotada a la empresa.
    boolean existsByCompany_IdAndSale_SaleIdAndCreditNoteStatusIn(Integer companyId, Integer saleId, List<String> statuses);

    // La ordenación por fecha ahora está acotada a la empresa.
    List<CreditNote> findByCompany_IdOrderByIssueDateDesc(Integer companyId);

    // La ordenación por estado ahora está acotada a la empresa.
    @Query("SELECT cn FROM CreditNote cn WHERE cn.company.id = :companyId ORDER BY " +
            "CASE cn.creditNoteStatus " +
            "  WHEN 'PENDIENTE' THEN 1 " +
            "  WHEN 'APLICADA'  THEN 2 " +
            "  WHEN 'ANULADA'   THEN 3 " +
            "  ELSE 4 " +
            "END, " +
            "cn.issueDate DESC")
    List<CreditNote> findAllByCompanyIdOrderByStatusAndCreditNoteDate(@Param("companyId") Integer companyId);

    // =========================================================================================
    // == FIN DE CÓDIGO MODIFICADO
    // =========================================================================================
}