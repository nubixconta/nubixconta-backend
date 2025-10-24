package com.nubixconta.modules.purchases.repository;

import com.nubixconta.modules.purchases.entity.IncomeTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeTaxRepository extends JpaRepository<IncomeTax, Integer> {

    // --- MÉTODOS DE VALIDACIÓN ---

    /**
     * Verifica si ya existe una retención de ISR con el mismo número de documento para la empresa actual.
     * Usado para prevenir duplicados al crear o actualizar.
     */
    boolean existsByCompany_IdAndDocumentNumber(Integer companyId, String documentNumber);

    /**
     * Verifica si ya existe una retención de ISR activa (PENDIENTE o APLICADA) para una compra específica.
     * Crucial para evitar aplicar múltiples retenciones a la misma compra.
     */
    boolean existsByCompany_IdAndPurchase_IdPurchaseAndIncomeTaxStatusIn(Integer companyId, Integer purchaseId, List<String> statuses);


    // --- MÉTODOS OPTIMIZADOS CON JOIN FETCH (para evitar N+1 queries) ---

    /**
     * Busca una retención por su ID, precargando eficientemente la compra y el proveedor asociado.
     * Ideal para los endpoints de detalle (GET /{id}).
     */
    @Query("SELECT it FROM IncomeTax it " +
            "LEFT JOIN FETCH it.purchase p " +
            "LEFT JOIN FETCH p.supplier " +
            "WHERE it.idIncomeTax = :idIncomeTax")
    Optional<IncomeTax> findByIdWithDetails(@Param("idIncomeTax") Integer id);

    /**
     * Busca todas las retenciones para la empresa actual, ordenadas por estado y luego por fecha.
     * Usa JOIN FETCH para un rendimiento óptimo en la vista de listado principal.
     */
    @Query("SELECT it FROM IncomeTax it " +
            "LEFT JOIN FETCH it.purchase p " +
            "LEFT JOIN FETCH p.supplier " +
            "WHERE it.company.id = :companyId " +
            "ORDER BY CASE it.incomeTaxStatus WHEN 'PENDIENTE' THEN 1 WHEN 'APLICADA' THEN 2 WHEN 'ANULADA' THEN 3 ELSE 4 END, it.issueDate DESC")
    List<IncomeTax> findAllWithDetailsByCompanyIdOrderByStatus(@Param("companyId") Integer companyId);

    /**
     * Busca todas las retenciones para la empresa actual, ordenadas por fecha de emisión descendente.
     * Alternativa de ordenamiento para la vista de listado.
     */
    @Query("SELECT it FROM IncomeTax it " +
            "LEFT JOIN FETCH it.purchase p " +
            "LEFT JOIN FETCH p.supplier " +
            "WHERE it.company.id = :companyId " +
            "ORDER BY it.issueDate DESC")
    List<IncomeTax> findAllWithDetailsByCompanyIdOrderByDate(@Param("companyId") Integer companyId);


    // --- MÉTODOS DE BÚSQUEDA Y FILTRADO ESPECÍFICOS ---

    /**
     * Busca todas las retenciones asociadas a un ID de compra específico.
     */
    List<IncomeTax> findByCompany_IdAndPurchase_IdPurchase(Integer companyId, Integer purchaseId);

    /**
     * Busca todas las retenciones por un estado específico (ej. "APLICADA").
     */
    List<IncomeTax> findByCompany_IdAndIncomeTaxStatus(Integer companyId, String status);

    /**
     * Busca retenciones para reportes, filtrando por rango de fechas y opcionalmente por estado.
     */
    @Query("SELECT it FROM IncomeTax it WHERE it.company.id = :companyId " +
            "AND it.issueDate >= :start AND it.issueDate <= :end " +
            "AND (:status IS NULL OR it.incomeTaxStatus = :status)")
    List<IncomeTax> findByCompanyIdAndDateRangeAndStatus(
            @Param("companyId") Integer companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") String status
    );
}