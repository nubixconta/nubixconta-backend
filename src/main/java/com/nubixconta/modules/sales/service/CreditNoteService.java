package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.sales.entity.CreditNote;
import com.nubixconta.modules.sales.repository.CreditNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreditNoteService {
    private final CreditNoteRepository creditNoteRepository;

    public List<CreditNote> findAll() {
        return creditNoteRepository.findAll();
    }

    public Optional<CreditNote> findById(Integer id) {
        return creditNoteRepository.findById(id);
    }

    public List<CreditNote> findBySaleId(Integer saleId) {
        return creditNoteRepository.findBySale_SaleId(saleId);
    }

    public CreditNote save(CreditNote creditNote) {
        return creditNoteRepository.save(creditNote);
    }

    public void delete(Integer id) {
        if (!creditNoteRepository.existsById(id)) {
            throw new IllegalArgumentException("Nota de crédito no encontrada");
        }
        creditNoteRepository.deleteById(id);
    }
}