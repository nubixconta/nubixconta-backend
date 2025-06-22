package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.sales.entity.CreditNote;
import com.nubixconta.modules.sales.repository.CreditNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


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

    public List<CreditNote> findByStatus(String status) {
        return creditNoteRepository.findByCreditNoteStatus(status);
    }

    // Buscar por rango de fechas (inicio y fin obligatorios) y opcionalmente por estado
    public List<CreditNote> findByDateRangeAndStatus(LocalDate start, LocalDate end, String status) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Debe enviar ambos parámetros: inicio y fin.");
        }
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        // Si status es nulo o vacío, pásalo como null para traer todos
        return creditNoteRepository.findByDateRangeAndStatus(startDateTime, endDateTime,
                (status != null && !status.isBlank()) ? status : null);
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