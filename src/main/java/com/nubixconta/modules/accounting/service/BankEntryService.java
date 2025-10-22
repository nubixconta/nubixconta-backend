package com.nubixconta.modules.accounting.service;

import com.nubixconta.modules.accounting.dto.BankEntryDTO;
import com.nubixconta.modules.accounting.entity.BankEntry;
import com.nubixconta.modules.accounting.repository.BankEntryRepository;
import com.nubixconta.modules.banks.entity.TransactionBank;
import com.nubixconta.modules.banks.repository.TransactionBankRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BankEntryService {

    private final BankEntryRepository repository;
    private final TransactionBankRepository transactionRepository;
    private final ModelMapper mapper = new ModelMapper();

    // Crear una nueva entrada contable
    public BankEntryDTO createEntry(BankEntryDTO dto) {
        TransactionBank transaction = transactionRepository.findById(dto.getTransactionBankId())
                .orElseThrow(() -> new EntityNotFoundException("Transacción bancaria no encontrada"));

        // Validar estado: solo transacciones pendientes pueden modificarse
        if (!"PENDIENTE".equalsIgnoreCase(transaction.getAccountingTransactionStatus())) {
            throw new IllegalStateException("No se pueden agregar movimientos a una transacción APLICADA o ANULADA.");
        }

        BankEntry entry = mapper.map(dto, BankEntry.class);
        entry.setTransactionBank(transaction);

        BankEntry saved = repository.save(entry);

        // Recalcular el totalAmount
        recalculateTransactionTotal(transaction.getIdBankTransaction());

        return mapper.map(saved, BankEntryDTO.class);
    }

    // Actualizar entrada existente
    public BankEntryDTO updateEntry(Integer id, BankEntryDTO dto) {
        BankEntry entry = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entrada contable no encontrada"));

        TransactionBank transaction = entry.getTransactionBank();

        // Validar estado: solo transacciones pendientes pueden modificarse
        if (!"PENDIENTE".equalsIgnoreCase(transaction.getAccountingTransactionStatus())) {
            throw new IllegalStateException("No se pueden modificar movimientos de una transacción APLICADA o ANULADA.");
        }

        if (dto.getIdCatalog() != null) {
            entry.setIdCatalog(dto.getIdCatalog());
        }
        if (dto.getDebit() != null) {
            entry.setDebit(dto.getDebit());
        }
        if (dto.getCredit() != null) {
            entry.setCredit(dto.getCredit());
        }
        if (dto.getDescription() != null) {
            entry.setDescription(dto.getDescription());
        }
        if (dto.getDate() != null) {
            entry.setDate(dto.getDate());
        }

        BankEntry updated = repository.save(entry);

        // Recalcular totalAmount
        recalculateTransactionTotal(entry.getTransactionBank().getIdBankTransaction());

        return mapper.map(updated, BankEntryDTO.class);
    }

    // Eliminar entrada
    public void deleteEntry(Integer id) {
        BankEntry entry = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entrada contable no encontrada"));

        TransactionBank transaction = entry.getTransactionBank();

        // Validar estado
        if (!"PENDIENTE".equalsIgnoreCase(transaction.getAccountingTransactionStatus())) {
            throw new IllegalStateException("No se pueden eliminar movimientos de una transacción APLICADA o ANULADA.");
        }

        repository.delete(entry);

        // Recalcular total
        recalculateTransactionTotal(transaction.getIdBankTransaction());
    }

    // Listar todas las entradas
    public List<BankEntryDTO> listAll() {
        return repository.findAll()
                .stream()
                .map(e -> mapper.map(e, BankEntryDTO.class))
                .collect(Collectors.toList());
    }

    // Listar por ID de transacción
    public List<BankEntryDTO> listByTransaction(Integer transactionId) {
        return repository.findByTransactionBank_IdBankTransaction(transactionId)
                .stream()
                .map(e -> mapper.map(e, BankEntryDTO.class))
                .collect(Collectors.toList());
    }

    // Buscar por ID
    public BankEntryDTO findById(Integer id) {
        BankEntry entry = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entrada contable no encontrada"));
        return mapper.map(entry, BankEntryDTO.class);
    }

    // Nuevo método para recalcular el totalAmount de la transacción
    private void recalculateTransactionTotal(Integer transactionId) {
        TransactionBank transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada para recalcular total"));

        // Sumamos los débitos y créditos
        BigDecimal totalDebits = repository.sumDebitsByTransactionId(transactionId);
        BigDecimal totalCredits = repository.sumCreditsByTransactionId(transactionId);

        // Si ambos son nulos, ponemos 0
        BigDecimal total = BigDecimal.ZERO;
        
        // Si la transacción es de tipo "ENTRADA", usa los débitos
        if ("ENTRADA".equalsIgnoreCase(transaction.getTransactionType())) {
            total = totalDebits != null ? totalDebits : BigDecimal.ZERO;
        }
        // Si es "SALIDA", usa los créditos
        else if ("SALIDA".equalsIgnoreCase(transaction.getTransactionType())) {
            total = totalCredits != null ? totalCredits : BigDecimal.ZERO;
        }

        // Actualizamos el totalAmount en la transacción
        transaction.setTotalAmount(total);
        transactionRepository.save(transaction);
    }
}
