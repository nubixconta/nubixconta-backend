package com.nubixconta.modules.purchases.repository;

import com.nubixconta.modules.purchases.entity.PurchaseCreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseCreditNoteRepository extends JpaRepository<PurchaseCreditNote, Integer> {

    boolean existsByCompany_IdAndDocumentNumber(Integer companyId, String documentNumber);

    boolean existsByCompany_IdAndPurchase_IdPurchaseAndCreditNoteStatusIn(Integer companyId, Integer purchaseId, List<String> statuses);

    List<PurchaseCreditNote> findByCompany_IdOrderByIssueDateDesc(Integer companyId);

    @Query("SELECT pcn FROM PurchaseCreditNote pcn WHERE pcn.company.id = :companyId ORDER BY " +
            "CASE pcn.creditNoteStatus " +
            "  WHEN 'PENDIENTE' THEN 1 " +
            "  WHEN 'APLICADA'  THEN 2 " +
            "  WHEN 'ANULADA'   THEN 3 " +
            "  ELSE 4 " +
            "END, " +
            "pcn.issueDate DESC")
    List<PurchaseCreditNote> findAllByCompanyIdOrderByStatusAndIssueDate(@Param("companyId") Integer companyId);
}