package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.accounting.dto.accounting.*;

import com.nubixconta.modules.accounting.entity.AccountingEntry;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.entity.TransactionAccounting;
import com.nubixconta.modules.accounting.entity.enums.AccountingTransactionStatus;
import com.nubixconta.modules.accounting.repository.TransactionAccountingRepository;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionAccountingService {

    private final TransactionAccountingRepository transactionRepository;
    private final CompanyRepository companyRepository;
    private final CatalogService catalogService;
    private final ModelMapper modelMapper;

    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("No se puede determinar la empresa del contexto de seguridad."));
    }

    @Transactional(readOnly = true)
    public TransactionAccountingResponseDTO findById(Long id) {
        Integer companyId = getCompanyIdFromContext();
        TransactionAccounting transaction = transactionRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Transacción contable con ID " + id + " no encontrada."));
        return mapToResponseDTO(transaction);
    }

    /**
     * Retorna todas las transacciones, aplicando un ordenamiento específico.
     * Replica la lógica de PurchaseService.
     */
    @Transactional(readOnly = true)
    public List<TransactionAccountingResponseDTO> findAll(String sortBy) {
        Integer companyId = getCompanyIdFromContext();
        List<TransactionAccounting> transactions;

        if ("status".equalsIgnoreCase(sortBy)) {
            // Llama al nuevo método que ordena por estado
            transactions = transactionRepository.findAllByCompanyIdOrderByStatusAndDate(companyId);
        } else {
            // El comportamiento por defecto (o si sortBy es 'date') es ordenar solo por fecha
            transactions = transactionRepository.findAllByCompanyIdOrderByTransactionDateDesc(companyId);
        }

        return transactions.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    /**
     * Busca transacciones contables utilizando una combinación de filtros.
     * @param startDate La fecha de inicio para el filtro (puede ser nula).
     * @param endDate La fecha de fin para el filtro (puede ser nula).
     * @param status El estado de la transacción para el filtro (puede ser nulo).
     * @return Una lista de DTOs de las transacciones encontradas.
     */
    @Transactional(readOnly = true)
    public List<TransactionAccountingResponseDTO> findByFilters(LocalDate startDate, LocalDate endDate, AccountingTransactionStatus status) {
        Integer companyId = getCompanyIdFromContext();

        // Convertimos LocalDate a LocalDateTime para cubrir el día completo, igual que en PurchaseService.
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        List<TransactionAccounting> transactions = transactionRepository.findByFilters(companyId, startDateTime, endDateTime, status);

        return transactions.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionAccountingResponseDTO createTransaction(TransactionAccountingCreateDTO dto) {
        Integer companyId = getCompanyIdFromContext();
        Company company = companyRepository.getReferenceById(companyId);

        // 1. Validaciones de negocio
        BigDecimal totalDebe = validatePartidaDoble(dto);

        // 2. Mapeo de DTO a Entidad
        TransactionAccounting newTransaction = new TransactionAccounting();
        newTransaction.setCompany(company);
        newTransaction.setTransactionDate(dto.getTransactionDate());
        newTransaction.setDescription(dto.getDescription());
        newTransaction.setStatus(AccountingTransactionStatus.PENDIENTE);
        newTransaction.setModuleType("CONTABILIDAD");
        newTransaction.setTotalDebe(totalDebe);
        newTransaction.setTotalHaber(totalDebe); // Es igual al debe por la validación

        // 3. Mapeo de las líneas
        for (AccountingEntryDTO entryDTO : dto.getEntries()) {
            Catalog catalog = catalogService.findEntityById(entryDTO.getCatalogId());
            if (!catalog.getCompany().getId().equals(companyId)) {
                throw new BusinessRuleException("La cuenta contable '" + catalog.getEffectiveName() + "' no pertenece a la empresa actual.");
            }

            AccountingEntry newEntry = new AccountingEntry();
            newEntry.setCatalog(catalog);
            newEntry.setDebe(entryDTO.getDebit());
            newEntry.setHaber(entryDTO.getCredit());
            newEntry.setDescription(dto.getDescription()); // Regla de negocio: Heredar descripción
            newEntry.setDate(dto.getTransactionDate());

            newTransaction.addEntry(newEntry);
        }

        TransactionAccounting savedTransaction = transactionRepository.save(newTransaction);
        return mapToResponseDTO(savedTransaction);
    }
    @Transactional
    public TransactionAccountingResponseDTO updateTransaction(Long transactionId, TransactionAccountingUpdateDTO dto) {
        Integer companyId = getCompanyIdFromContext();
        TransactionAccounting transaction = transactionRepository.findByIdAndCompanyId(transactionId, companyId)
                .orElseThrow(() -> new NotFoundException("Transacción contable con ID " + transactionId + " no encontrada."));

        // REGLA 1: Solo se pueden editar transacciones PENDIENTES.
        if (transaction.getStatus() != AccountingTransactionStatus.PENDIENTE) {
            throw new BusinessRuleException("Solo se pueden editar transacciones con estado PENDIENTE. Estado actual: " + transaction.getStatus());
        }

        // REGLA 2: Si se envían nuevas líneas, se debe validar la partida doble.
        if (dto.getEntries() != null) {
            validatePartidaDobleForUpdate(dto);
        }

        // Actualizar campos de la cabecera si se proporcionaron
        if (dto.getTransactionDate() != null) {
            transaction.setTransactionDate(dto.getTransactionDate());
        }
        if (dto.getDescription() != null) {
            transaction.setDescription(dto.getDescription());
        }

        // Lógica de sincronización de detalles (si se proporcionan)
        if (dto.getEntries() != null) {
            // Calculamos y actualizamos los nuevos totales
            BigDecimal newTotal = calculateTotalDebe(dto.getEntries());
            transaction.setTotalDebe(newTotal);
            transaction.setTotalHaber(newTotal);

            // PATRÓN DE RECONSTRUCCIÓN:
            // Limpiamos la colección actual y la reconstruimos desde el DTO.
            // Gracias a `orphanRemoval = true`, JPA/Hibernate eliminará de la BD las líneas antiguas.
            transaction.getAccountingEntries().clear();

            for (AccountingEntryDTO entryDTO : dto.getEntries()) {
                Catalog catalog = catalogService.findEntityById(entryDTO.getCatalogId());
                if (!catalog.getCompany().getId().equals(companyId)) {
                    throw new BusinessRuleException("La cuenta contable '" + catalog.getEffectiveName() + "' no pertenece a la empresa actual.");
                }

                AccountingEntry newEntry = new AccountingEntry();
                newEntry.setCatalog(catalog);
                newEntry.setDebe(entryDTO.getDebit());
                newEntry.setHaber(entryDTO.getCredit());
                // La descripción de las líneas se actualiza con la nueva descripción de la cabecera
                newEntry.setDescription(transaction.getDescription());
                // La fecha de las líneas se actualiza con la nueva fecha de la cabecera
                newEntry.setDate(transaction.getTransactionDate());

                transaction.addEntry(newEntry);
            }
        }

        TransactionAccounting updatedTransaction = transactionRepository.save(transaction);
        return mapToResponseDTO(updatedTransaction);
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        Integer companyId = getCompanyIdFromContext();
        TransactionAccounting transaction = transactionRepository.findByIdAndCompanyId(transactionId, companyId)
                .orElseThrow(() -> new NotFoundException("Transacción contable con ID " + transactionId + " no encontrada."));

        if (transaction.getStatus() != AccountingTransactionStatus.PENDIENTE) {
            throw new BusinessRuleException("Solo se pueden eliminar transacciones en estado PENDIENTE.");
        }

        transactionRepository.delete(transaction);
    }

    @Transactional
    public TransactionAccountingResponseDTO applyTransaction(Long transactionId) {
        Integer companyId = getCompanyIdFromContext();
        TransactionAccounting transaction = transactionRepository.findByIdAndCompanyId(transactionId, companyId)
                .orElseThrow(() -> new NotFoundException("Transacción contable con ID " + transactionId + " no encontrada."));

        if (transaction.getStatus() != AccountingTransactionStatus.PENDIENTE) {
            throw new BusinessRuleException("Solo se pueden aplicar transacciones en estado PENDIENTE.");
        }

        transaction.setStatus(AccountingTransactionStatus.APLICADA);
        TransactionAccounting appliedTransaction = transactionRepository.save(transaction);
        return mapToResponseDTO(appliedTransaction);
    }

    @Transactional
    public TransactionAccountingResponseDTO cancelTransaction(Long transactionId) {
        Integer companyId = getCompanyIdFromContext();
        TransactionAccounting transaction = transactionRepository.findByIdAndCompanyId(transactionId, companyId)
                .orElseThrow(() -> new NotFoundException("Transacción contable con ID " + transactionId + " no encontrada."));

        if (transaction.getStatus() != AccountingTransactionStatus.APLICADA) {
            throw new BusinessRuleException("Solo se pueden anular transacciones en estado APLICADA.");
        }

        // 1. ¡LÍNEA CLAVE! Elimina todas las líneas asociadas de la colección.
        // Gracias a 'orphanRemoval = true' en la entidad, JPA se encargará de ejecutar
        // las sentencias DELETE en la base de datos para cada una de estas líneas.
        transaction.getAccountingEntries().clear();
        
        transaction.setStatus(AccountingTransactionStatus.ANULADA);
        TransactionAccounting cancelledTransaction = transactionRepository.save(transaction);
        return mapToResponseDTO(cancelledTransaction);
    }

    // --- Métodos Privados de Ayuda ---

    private BigDecimal validatePartidaDoble(TransactionAccountingCreateDTO dto) {
        BigDecimal totalDebe = dto.getEntries().stream()
                .map(e -> e.getDebit().setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalHaber = dto.getEntries().stream()
                .map(e -> e.getCredit().setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new BusinessRuleException("Partida descuadrada. Total Debe: " + totalDebe + ", Total Haber: " + totalHaber);
        }

        if (totalDebe.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("La partida debe tener un valor mayor a cero.");
        }
        return totalDebe;
    }

    private TransactionAccountingResponseDTO mapToResponseDTO(TransactionAccounting transaction) {
        TransactionAccountingResponseDTO dto = modelMapper.map(transaction, TransactionAccountingResponseDTO.class);
        dto.setEntries(
                transaction.getAccountingEntries().stream()
                        .map(entry -> new JournalEntryLineResponseDTO( // <--- USA EL CONSTRUCTOR DEL DTO RENOMBRADO
                                entry.getId(),
                                entry.getCatalog().getId(),
                                entry.getCatalog().getEffectiveCode(),
                                entry.getCatalog().getEffectiveName(),
                                entry.getDebe(),
                                entry.getHaber(),
                                entry.getDescription()
                        ))
                        .collect(Collectors.toSet())
        );
        return dto;
    }

    private void validatePartidaDobleForUpdate(TransactionAccountingUpdateDTO dto) {
        if (dto.getEntries() == null) return;
        BigDecimal totalDebe = calculateTotalDebe(dto.getEntries());
        BigDecimal totalHaber = calculateTotalHaber(dto.getEntries());

        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new BusinessRuleException("Partida descuadrada. Total Debe: " + totalDebe + ", Total Haber: " + totalHaber);
        }
        if (totalDebe.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("La partida debe tener un valor mayor a cero.");
        }
    }

    private BigDecimal calculateTotalDebe(Set<AccountingEntryDTO> entries) {
        return entries.stream()
                .map(e -> e.getDebit().setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalHaber(Set<AccountingEntryDTO> entries) {
        return entries.stream()
                .map(e -> e.getCredit().setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}