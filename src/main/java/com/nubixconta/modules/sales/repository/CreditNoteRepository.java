package com.nubixconta.modules.sales.repository;

import com.nubixconta.modules.sales.entity.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Integer> {

    // Buscar todas las notas de crédito de una venta específica
    List<CreditNote> findBySale_SaleId(Integer saleId);

    // Buscar por estado exacto (APLICADA, PENDIENTE, ANULADA)
    List<CreditNote> findByCreditNoteStatus(String status);

    // Buscar por rango de fechas y, opcionalmente, por estado
    @Query("SELECT n FROM CreditNote n WHERE " +
            "n.creditNoteDate >= :start AND n.creditNoteDate <= :end " +
            "AND (:status IS NULL OR n.creditNoteStatus = :status)")
    List<CreditNote> findByDateRangeAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") String status
    );

    // Método para validar unicidad del número de documento
    boolean existsByDocumentNumber(String documentNumber);

    // --- MÉTODO CLAVE A AÑADIR ---
    /**
     * Verifica si existe al menos una nota de crédito para una venta dada que
     * se encuentre en uno de los estados proporcionados.
     * @param saleId El ID de la venta a verificar.
     * @param statuses La lista de estados a buscar (ej. ["PENDIENTE", "APLICADA"]).
     * @return true si se encuentra al menos una, false en caso contrario.
     */
    boolean existsBySale_SaleIdAndCreditNoteStatusIn(Integer saleId, List<String> statuses);

}