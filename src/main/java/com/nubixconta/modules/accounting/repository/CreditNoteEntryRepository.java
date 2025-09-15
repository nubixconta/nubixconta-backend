package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.CreditNoteEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditNoteEntryRepository extends JpaRepository<CreditNoteEntry, Integer> {

    // Método para eliminar todos los asientos de una nota de crédito al anularla.
    void deleteByCreditNote_IdNotaCredit(Integer creditNoteId);

    /**
     * Busca todas las líneas del asiento para una nota de crédito específica, cargando
     * eficientemente las entidades anidadas Catalog y Account para evitar N+1.
     * @param creditNoteId El ID de la nota de crédito.
     * @return Una lista de las entidades CreditNoteEntry con sus datos relacionados precargados.
     */
    @Query("SELECT cne FROM CreditNoteEntry cne " +
            "JOIN FETCH cne.catalog c " +
            "JOIN FETCH c.account " +
            "WHERE cne.creditNote.idNotaCredit = :creditNoteId")
    List<CreditNoteEntry> findByCreditNoteIdWithDetails(Integer creditNoteId);
}
