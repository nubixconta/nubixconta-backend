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

        BankEntry entry = mapper.map(dto, BankEntry.class);
        entry.setTransactionBank(transaction);

        BankEntry saved = repository.save(entry);
        return mapper.map(saved, BankEntryDTO.class);
    }

    // Actualizar entrada existente
    public BankEntryDTO updateEntry(Integer id, BankEntryDTO dto) {
        BankEntry entry = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entrada contable no encontrada"));

        entry.setIdCatalog(dto.getIdCatalog());
        entry.setDebit(dto.getDebit());
        entry.setCredit(dto.getCredit());
        entry.setDescription(dto.getDescription());
        entry.setDate(dto.getDate());

        BankEntry updated = repository.save(entry);
        return mapper.map(updated, BankEntryDTO.class);
    }

    // Eliminar entrada
    public void deleteEntry(Integer id) {
        BankEntry entry = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entrada contable no encontrada"));
        repository.delete(entry);
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
}
