package com.nubixconta.modules.banks.service;

import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.banks.dto.TransactionBankDTO;
import com.nubixconta.modules.banks.entity.TransactionBank;
import com.nubixconta.modules.banks.repository.TransactionBankRepository;
import com.nubixconta.modules.accounting.entity.BankEntry;
import com.nubixconta.modules.accounting.repository.BankEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionBankService {

    private final TransactionBankRepository repository;
    private final BankEntryRepository bankEntryRepository;
    private final ModelMapper mapper = new ModelMapper();
    private final CompanyRepository companyRepository;

    // Crear una nueva transacción
    public TransactionBankDTO createTransaction(TransactionBankDTO dto) {
        TransactionBank entity = mapper.map(dto, TransactionBank.class);

        entity.setAccountingTransactionStatus("PENDIENTE");
        entity.setModuleType("BANCOS"); // siempre es el módulo actual

        if (entity.getBankEntries() != null) {
            for (BankEntry entry : entity.getBankEntries()) {
                entry.setTransactionBank(entity); // Set the parent reference on each child
                
                // Optional but good practice: Set default date if not provided
                if (entry.getDate() == null) {
                    entry.setDate(LocalDateTime.now()); 
                }
            }
        }

        BigDecimal total = calculateTotalAmount(entity.getTransactionType(), entity.getBankEntries());
        entity.setTotalAmount(total);

        entity.setTotalAmount(BigDecimal.ZERO); // inicia en 0 hasta que se creen los asientos

        // Asignar empresa manualmente 
        entity.setCompany(
            companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"))
        );

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

        if (dto.getTransactionType() != null) {
            entity.setTransactionType(dto.getTransactionType());
        }
        if (dto.getReceiptNumber() != null) {
            entity.setReceiptNumber(dto.getReceiptNumber());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(dto.getTransactionDate());
        }

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

        // Verificar que cumpla partida doble
        BigDecimal totalDebits = bankEntryRepository.sumDebitsByTransactionId(id);
        BigDecimal totalCredits = bankEntryRepository.sumCreditsByTransactionId(id);

        if (totalDebits == null) totalDebits = BigDecimal.ZERO;
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalStateException("La transacción no cumple con la partida doble: débitos y créditos no son iguales.");
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

        // Eliminar todos los movimientos contables asociados a la transacción
        if (entity.getBankEntries() != null && !entity.getBankEntries().isEmpty()) {
            entity.getBankEntries().clear(); // cascade = ALL + orphanRemoval = true eliminará del DB
        }

        entity.setAccountingTransactionStatus("ANULADA");
        TransactionBank updated = repository.save(entity);
        return mapper.map(updated, TransactionBankDTO.class);
    }

    /**
     * Obtener todas las transacciones (con filtros dinámicos)
     */
    public List<TransactionBankDTO> listAll(Integer idCatalog, LocalDate dateFrom, LocalDate dateTo) {
        
        // Inicia una consulta vacía (trae todo)
        Specification<TransactionBank> spec = Specification.where(null);

        // Añade el filtro de CÓDIGO DE CUENTA (si se proporcionó)
        if (idCatalog != null) {
            spec = spec.and(hasAccountCode(idCatalog));
        }

        // Añade los filtros de FECHA
        if (dateFrom != null && dateTo != null) {
            // Caso 1: Rango de fechas (ambas presentes)
            spec = spec.and(transactionDateAfter(dateFrom.atStartOfDay()));
            spec = spec.and(transactionDateBefore(dateTo.atTime(LocalTime.MAX)));

        } else if (dateFrom != null) {
            // Caso 2: Solo una fecha (buscar solo ESE día)
            spec = spec.and(transactionDateAfter(dateFrom.atStartOfDay()));
            spec = spec.and(transactionDateBefore(dateFrom.atTime(LocalTime.MAX)));
        
        } else if (dateTo != null) {
            // Caso 3: Solo fecha de fin (buscar todo HASTA el final de ese día)
            spec = spec.and(transactionDateBefore(dateTo.atTime(LocalTime.MAX)));
        }

        // Ejecuta la consulta en el repositorio usando las especificaciones
        List<TransactionBank> results = repository.findAll(spec);

        // Mapea a DTO y devuelve
        return results.stream()
                .map(entity -> mapper.map(entity, TransactionBankDTO.class))
                .collect(Collectors.toList());
    }

    // Buscar por ID
    public TransactionBankDTO findById(Integer id) {
        TransactionBank entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada"));
        return mapper.map(entity, TransactionBankDTO.class);
    }

    /**
     * Calcula el monto total BASADO EN LOS ASIENTOS EN MEMORIA y el tipo de transacción.
     * Adaptado de la lógica de recalculateTransactionTotal.
     *
     * @param transactionType El tipo de transacción ("ENTRADA" o "SALIDA").
     * @param entries La colección de BankEntry (aún no guardados).
     * @return El monto total calculado.
     */
    private BigDecimal calculateTotalAmount(String transactionType, List<BankEntry> entries) { 
        if (entries == null || entries.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        // Sumamos débitos y créditos DE LA LISTA EN MEMORIA
        for (BankEntry entry : entries) {
            if (entry.getDebit() != null) {
                totalDebits = totalDebits.add(entry.getDebit());
            }
            if (entry.getCredit() != null) {
                totalCredits = totalCredits.add(entry.getCredit());
            }
        }

        // Aplicamos la lógica ENTRADA/SALIDA
        if ("ENTRADA".equalsIgnoreCase(transactionType)) {
            return totalDebits;
        } else if ("SALIDA".equalsIgnoreCase(transactionType)) {
            return totalCredits;
        } else {
            // Comportamiento por defecto si el tipo no es ENTRADA/SALIDA (ej. diferencia)
            // O podrías lanzar un error si el tipo es inválido.
            return totalDebits.subtract(totalCredits); // O simplemente BigDecimal.ZERO
        }
    }

    /**
     * Crea una especificación para encontrar transacciones que tengan un BankEntry con el idCatalog dado.
     */
    private Specification<TransactionBank> hasAccountCode(Integer idCatalog) {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                query.distinct(true); // Evita duplicados
            }
            // Une TransactionBank con su lista de BankEntry
            Join<TransactionBank, BankEntry> entryJoin = root.join("bankEntries"); 
            // Compara el campo "idCatalog" dentro de la entidad unida (BankEntry)
            return cb.equal(entryJoin.get("idCatalog"), idCatalog);
        };
    }

    /**
     * Crea una especificación para encontrar transacciones DESPUÉS de una fecha/hora.
     */
    private Specification<TransactionBank> transactionDateAfter(LocalDateTime startDate) {
        return (root, query, cb) -> 
            cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate);
    }

    /**
     * Crea una especificación para encontrar transacciones ANTES de una fecha/hora.
     */
    private Specification<TransactionBank> transactionDateBefore(LocalDateTime endDate) {
        return (root, query, cb) -> 
            cb.lessThanOrEqualTo(root.get("transactionDate"), endDate);
    }
}
