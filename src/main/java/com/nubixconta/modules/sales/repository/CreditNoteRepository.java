package com.nubixconta.modules.sales.repository;

import com.nubixconta.modules.sales.entity.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Integer> {
    // Buscar todas las notas de crédito de una venta específica
    List<CreditNote> findBySale_SaleId(Integer saleId);
}