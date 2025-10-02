package com.nubixconta.modules.purchases.repository;

import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.modules.sales.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {

    // --- Métodos de Validación y Búsqueda Básica (Espejo de SaleRepository) ---

    boolean existsByCompany_IdAndDocumentNumber(Integer companyId, String documentNumber);

    /**
     * Busca una compra por el ID de la empresa y el número de documento.
     */
    Optional<Purchase> findByCompany_IdAndDocumentNumber(Integer companyId, String documentNumber);

    List<Purchase> findByCompany_IdOrderByIssueDateDesc(Integer companyId);

    List<Purchase> findByCompany_IdAndPurchaseStatus(Integer companyId, String purchaseStatus);

    // Ordenamiento por estado, idéntico al de Ventas
    @Query("SELECT p FROM Purchase p WHERE p.company.id = :companyId ORDER BY " +
            "CASE p.purchaseStatus " +
            "  WHEN 'PENDIENTE' THEN 1 " +
            "  WHEN 'APLICADA'  THEN 2 " +
            "  WHEN 'ANULADA'   THEN 3 " +
            "  ELSE 4 " +
            "END, " +
            "p.issueDate DESC")
    List<Purchase> findAllByCompanyIdOrderByStatusAndIssueDate(@Param("companyId") Integer companyId);

    // --- Métodos para Reportes (Espejo de SaleRepository) ---

    @Query(value = "SELECT p.* FROM purchase p JOIN supplier s ON p.id_supplier = s.id_supplier WHERE " +
            "p.company_id = :companyId AND " +
            "p.purchase_status = 'APLICADA' AND " +
            "p.issue_date >= COALESCE(:startDate, p.issue_date) AND " +
            "p.issue_date <= COALESCE(:endDate, p.issue_date) AND " +
            "COALESCE(LOWER(CAST(s.suplier_name AS VARCHAR)), '') LIKE LOWER(CONCAT('%', COALESCE(:supplierName, ''), '%')) AND " +
            "COALESCE(LOWER(CAST(s.suplier_last_name AS VARCHAR)), '') LIKE LOWER(CONCAT('%', COALESCE(:supplierLastName, ''), '%')) " +
            "ORDER BY p.issue_date DESC",
            nativeQuery = true)
    List<Purchase> findByCombinedCriteria(
            @Param("companyId") Integer companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("supplierName") String supplierName,
            @Param("supplierLastName") String supplierLastName
    );
    // Busca una venta por su ID y el ID de la empresa.
    Optional<Purchase> findByIdPurchaseAndCompanyId(Integer purchaseId, Integer companyId);
}