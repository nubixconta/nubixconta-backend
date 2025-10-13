package com.nubixconta.modules.banks.service;

import com.nubixconta.modules.banks.dto.TransactionBankDTO;
import com.nubixconta.modules.banks.entity.TransactionBank;
import com.nubixconta.modules.banks.repository.TransactionBankRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionBankService {

    private final TransactionBankRepository repository;
    private final ModelMapper mapper = new ModelMapper();

    // Crear una nueva transacción
    public TransactionBankDTO createTransaction(TransactionBankDTO dto) {
        TransactionBank entity = mapper.map(dto, TransactionBank.class);
        entity.setAccountingTransactionStatus("PENDIENTE");
        TransactionBank saved = repository.save(entity);
        return mapper.map(saved, TransactionBankDTO.class);
    }

    // Actualizar transacción pendiente
    public TransactionBankDTO updateTransaction(Integer id, TransactionBankDTO dto) {
        TransactionBank entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada"));

        if (!"PENDIENTE".equalsIgnoreCase(entity.getAccountingTransactionStatus())) {
            throw new IllegalStateException("Solo se pueden editar transacciones pendientes");
        }

        entity.setTransactionType(dto.getTransactionType());
        entity.setReceiptNumber(dto.getReceiptNumber());
        entity.setDescription(dto.getDescription());
        entity.setTransactionDate(dto.getTransactionDate());
        TransactionBank updated = repository.save(entity);
        return mapper.map(updated, TransactionBankDTO.class);
    }

    // Eliminar transacción pendiente
    public void deleteTransaction(Integer id) {
        TransactionBank entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada"));

        if (!"PENDIENTE".equalsIgnoreCase(entity.getAccountingTransactionStatus())) {
            throw new IllegalStateException("Solo se pueden eliminar transacciones pendientes");
        }

        repository.delete(entity);
    }

    // Aplicar transacción (cambia a estado "APLICADA")
    public TransactionBankDTO applyTransaction(Integer id) {
        TransactionBank entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada"));

        if (!"PENDIENTE".equalsIgnoreCase(entity.getAccountingTransactionStatus())) {
            throw new IllegalStateException("Solo se pueden aplicar transacciones pendientes");
        }

        entity.setAccountingTransactionStatus("APLICADA");
        TransactionBank updated = repository.save(entity);
        return mapper.map(updated, TransactionBankDTO.class);
    }

    // Anular transacción aplicada (cambia a estado "ANULADA")
    public TransactionBankDTO annulTransaction(Integer id) {
        TransactionBank entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada"));

        if (!"APLICADA".equalsIgnoreCase(entity.getAccountingTransactionStatus())) {
            throw new IllegalStateException("Solo se pueden anular transacciones aplicadas");
        }

        entity.setAccountingTransactionStatus("ANULADA");
        TransactionBank updated = repository.save(entity);
        return mapper.map(updated, TransactionBankDTO.class);
    }

    // Obtener todas las transacciones
    public List<TransactionBankDTO> listAll() {
        return repository.findAll()
                .stream()
                .map(entity -> mapper.map(entity, TransactionBankDTO.class))
                .collect(Collectors.toList());
    }

    // Buscar por ID
    public TransactionBankDTO findById(Integer id) {
        TransactionBank entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada"));
        return mapper.map(entity, TransactionBankDTO.class);
    }
}
