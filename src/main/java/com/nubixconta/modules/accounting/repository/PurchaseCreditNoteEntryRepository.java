package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.PurchaseCreditNoteEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseCreditNoteEntryRepository extends JpaRepository<PurchaseCreditNoteEntry, Integer> {

    /**
     * Elimina todas las líneas de asiento asociadas a un ID de nota de crédito de compra específico.
     * Esencial para la operación de anulación, sigue el patrón de los otros repositorios contables.
     */
    void deleteByPurchaseCreditNote_IdPurchaseCreditNote(Integer creditNoteId);

    /**
     * Busca todas las líneas del asiento para una nota de crédito de compra específica, cargando
     * eficientemente las entidades anidadas Catalog y Account para evitar N+1.
     * @param creditNoteId El ID de la nota de crédito de compra.
     * @return Una lista de las entidades PurchaseCreditNoteEntry con sus datos relacionados precargados.
     */
    @Query("SELECT pcne FROM PurchaseCreditNoteEntry pcne " +
            "JOIN FETCH pcne.catalog c " +
            "JOIN FETCH c.account " +
            "WHERE pcne.purchaseCreditNote.idPurchaseCreditNote = :creditNoteId")
    List<PurchaseCreditNoteEntry> findByPurchaseCreditNoteIdWithDetails(Integer creditNoteId);
}