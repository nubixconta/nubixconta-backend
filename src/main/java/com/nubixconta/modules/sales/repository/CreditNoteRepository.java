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
    boolean existsBySale_SaleId(Integer saleId);
}