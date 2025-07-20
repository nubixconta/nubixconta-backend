package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.CreditNoteEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteEntryRepository extends JpaRepository<CreditNoteEntry, Integer> {

    // Método para eliminar todos los asientos de una nota de crédito al anularla.
    void deleteByCreditNote_IdNotaCredit(Integer creditNoteId);
}
