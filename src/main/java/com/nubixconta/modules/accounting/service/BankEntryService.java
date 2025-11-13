package com.nubixconta.modules.accounting.service;

import com.nubixconta.modules.accounting.dto.BankEntryDTO;
import com.nubixconta.modules.accounting.dto.bank.BankEntryResponseDTO;
import com.nubixconta.modules.accounting.entity.BankEntry;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.repository.BankEntryRepository;
import com.nubixconta.modules.accounting.repository.CatalogRepository;
import com.nubixconta.modules.banks.entity.TransactionBank;
import com.nubixconta.modules.banks.repository.TransactionBankRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BankEntryService {

    private final BankEntryRepository repository;
    private final TransactionBankRepository transactionRepository;
    private final ModelMapper mapper = new ModelMapper();
    private final CatalogRepository catalogRepository;

    // Crear una nueva entrada contable

    public BankEntryDTO createEntry(BankEntryDTO dto) {
        // Asegúrate de que el DTO tenga transactionBankId para este método
        if (dto.getTransactionBankId() == null) {
            throw new IllegalArgumentException("El ID de la transacción bancaria es obligatorio para crear un asiento.");
        }

        TransactionBank transaction = transactionRepository.findById(dto.getTransactionBankId())
                .orElseThrow(() -> new EntityNotFoundException("Transacción bancaria no encontrada con ID: " + dto.getTransactionBankId()));

        // Validar estado: solo transacciones pendientes pueden modificarse
        if (!"PENDIENTE".equalsIgnoreCase(transaction.getAccountingTransactionStatus())) {
            throw new IllegalStateException("No se pueden agregar movimientos a una transacción APLICADA o ANULADA.");
        }

        BankEntry entry = mapper.map(dto, BankEntry.class);
        entry.setTransactionBank(transaction);

        // Obtener la referencia al Catalog (idCatalog)
        Catalog catalog = catalogRepository.findById(dto.getIdCatalog())
                .orElseThrow(() -> new EntityNotFoundException("Catálogo de cuenta no encontrado con ID: " + dto.getIdCatalog()));
        entry.setIdCatalog(catalog);

        // Si la fecha no está establecida, usar la actual
        if (entry.getDate() == null) {
            entry.setDate(LocalDateTime.now());
        }

        BankEntry saved = repository.save(entry);



        return mapper.map(saved, BankEntryDTO.class);
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
    public List<BankEntryResponseDTO> listAll(String accountName, LocalDate startDate, LocalDate endDate) {
        // 1. Obtener todas las entidades desde la base de datos
        List<BankEntry> allEntries = repository.findAll();

        // 2. Mapear a DTO enriquecido y aplicar filtros en memoria
        return allEntries.stream()
                // Mapeamos cada entidad BankEntry a nuestro nuevo BankEntryResponseDTO
                .map(entry -> {
                    BankEntryResponseDTO dto = mapper.map(entry, BankEntryResponseDTO.class);
                    // Obtenemos el nombre de la cuenta desde el objeto Catalog asociado
                    if (entry.getIdCatalog() != null) {
                        dto.setAccountName(entry.getIdCatalog().getAccount().getAccountName());
                    }
                    return dto;
                })
                // 3. Aplicar filtros (la misma lógica que tu otro endpoint)
                .filter(dto -> accountName == null || accountName.trim().isEmpty() ||
                        (dto.getAccountName() != null && dto.getAccountName().toLowerCase().contains(accountName.toLowerCase())))
                .filter(dto -> startDate == null || !dto.getDate().toLocalDate().isBefore(startDate))
                .filter(dto -> endDate == null || !dto.getDate().toLocalDate().isAfter(endDate))
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
