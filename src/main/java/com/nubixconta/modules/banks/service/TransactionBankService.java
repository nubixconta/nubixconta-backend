package com.nubixconta.modules.banks.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.accounting.repository.CollectionEntryRepository;
import com.nubixconta.modules.accounting.service.BankEntryService;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.repository.CollectionDetailRepository;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.banks.dto.TransactionBankCreateRequestDTO;
import com.nubixconta.modules.banks.dto.TransactionBankDTO;
import com.nubixconta.modules.banks.entity.TransactionBank;
import com.nubixconta.modules.banks.repository.TransactionBankRepository;
import com.nubixconta.modules.accounting.dto.BankEntryDTO;
import com.nubixconta.modules.accounting.entity.BankEntry;
import com.nubixconta.modules.accounting.repository.BankEntryRepository;
import com.nubixconta.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionBankService {

    private final CollectionDetailRepository Collectionrepository;
    private final TransactionBankRepository repository;
    private final BankEntryRepository bankEntryRepository;
    private final ModelMapper mapper = new ModelMapper();
    private final BankEntryService bankEntryService;
    private final CompanyRepository companyRepository;


    // Helper privado para obtener el contexto de la empresa de forma segura y consistente.
    public Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }
    // --- Métodos de Listado y Búsqueda Existentes ---
    public List<CollectionDetail> findAll() {
        Integer companyId = getCompanyIdFromContext();
        return Collectionrepository.findByCompanyId(companyId);
    }

    private TransactionBankDTO mapToDTO(TransactionBank transactionBank) {
        // Implementa tu lógica de mapeo aquí.
        // Asegúrate de que los BankEntryDTOs se mapeen correctamente también.
        // Ejemplo simplificado:
        return TransactionBankDTO.builder()
                .idBankTransaction(transactionBank.getIdBankTransaction())
                .transactionDate(transactionBank.getTransactionDate())
                .totalAmount(transactionBank.getTotalAmount())
                .transactionType(transactionBank.getTransactionType())
                .receiptNumber(transactionBank.getReceiptNumber())
                .description(transactionBank.getDescription())
                .accountingTransactionStatus(transactionBank.getAccountingTransactionStatus())
                .moduleType(transactionBank.getModuleType())
                .companyId(transactionBank.getCompany() != null ? transactionBank.getCompany().getId() : null)

                // Mapear BankEntryDTOs
                .bankEntries(transactionBank.getBankEntries().stream()
                        .map(this::mapBankEntryToDTO) // Necesitarías un método similar para BankEntry
                        .collect(Collectors.toList()))
                .build();
    }

    private BankEntryDTO mapBankEntryToDTO(BankEntry bankEntry) {
        return BankEntryDTO.builder()
                .idBankEntry(bankEntry.getIdBankEntry())
                .idCatalog(bankEntry.getIdCatalog() != null ? bankEntry.getIdCatalog().getId() : null)
                .debit(bankEntry.getDebit())
                .credit(bankEntry.getCredit())
                .description(bankEntry.getDescription())
                .date(bankEntry.getDate())
                .transactionBankId(bankEntry.getTransactionBank() != null ? bankEntry.getTransactionBank().getIdBankTransaction() : null)
                .build();
    }


    public List<TransactionBankDTO> findTransactionsByAccountFilter(String accountFilter) {
        List<TransactionBank> transactions = repository.findByAccountNameOrCodeContainingIgnoreCase(accountFilter);
        return transactions.stream()
                .map(this::mapToDTO) // Usa tu método de mapeo de entidad a DTO
                .collect(Collectors.toList());
    }


    /**
     * Crea una nueva transacción bancaria completa (encabezado y asientos de detalle).
     * Este método es el nuevo endpoint principal para la creación.
     *
     * @param requestDto DTO con los datos de la transacción y sus asientos.
     * @return DTO de la transacción bancaria creada.
     */
    public TransactionBankDTO createFullTransaction(TransactionBankCreateRequestDTO requestDto) {
        Integer companyId = getCompanyIdFromContext();

        // Validar que el número de referencia no exista
        if (repository.existsByReceiptNumber(requestDto.getReceiptNumber())) {
            throw new IllegalArgumentException("El número de referencia '" + requestDto.getReceiptNumber() + "' ya existe para esta empresa.");
        }

        TransactionBank transactionBank = new TransactionBank();
        transactionBank.setTransactionDate(requestDto.getTransactionDate());
        transactionBank.setTransactionType(requestDto.getTransactionType());
        transactionBank.setReceiptNumber(requestDto.getReceiptNumber());
        transactionBank.setDescription(requestDto.getDescription());
        transactionBank.setAccountingTransactionStatus("PENDIENTE");
        transactionBank.setModuleType("BANCOS");
        if (requestDto.getTotalAmount() == null) {
            transactionBank.setTotalAmount(BigDecimal.ZERO);
        } else {
            transactionBank.setTotalAmount(requestDto.getTotalAmount());
        }

        Company companyRef = companyRepository.getReferenceById(companyId);
        transactionBank.setCompany(companyRef);

        TransactionBank savedTransactionBank = repository.save(transactionBank);

        // Validar que haya asientos
        if (requestDto.getBankEntries() == null || requestDto.getBankEntries().isEmpty()) {
            throw new IllegalArgumentException("La transacción debe contener al menos un asiento contable.");
        }

        for (BankEntryDTO entryDto : requestDto.getBankEntries()) {
            entryDto.setTransactionBankId(savedTransactionBank.getIdBankTransaction());
            // Llama a createEntry, que ahora NO recalculará el total de la TransactionBank.
            bankEntryService.createEntry(entryDto);
            // Ya no sumamos `finalTotalAmount` aquí porque no queremos que el backend lo recalcule ni lo actualice.
        }


        TransactionBank fullyLoadedTransaction = repository.findById(savedTransactionBank.getIdBankTransaction())
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada después de la creación."));

        return mapper.map(fullyLoadedTransaction, TransactionBankDTO.class);
    }

    // Actualizar transacción pendiente
    public TransactionBankDTO updateTransaction(Integer id, TransactionBankDTO dto) {
        TransactionBank entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada"));

        if (!"PENDIENTE".equalsIgnoreCase(entity.getAccountingTransactionStatus())) {
            throw new IllegalStateException("Solo se pueden editar transacciones pendientes");
        }

        // --- VALIDACIÓN DE UNICIDAD AL ACTUALIZAR ---
        if (dto.getReceiptNumber() != null && !dto.getReceiptNumber().equals(entity.getReceiptNumber())) {
            // El número de recibo cambió, verifica si el NUEVO número ya existe en OTRA transacción
            Optional<TransactionBank> existingWithNewNumber = repository.findByReceiptNumber(dto.getReceiptNumber());

            // Si existe una transacción con el nuevo número Y NO es la que estamos editando
            if (existingWithNewNumber.isPresent() && !existingWithNewNumber.get().getIdBankTransaction().equals(id)) {
                throw new IllegalArgumentException("El número de referencia '" + dto.getReceiptNumber() + "' ya está en uso por otra transacción.");
            }
            // Si no existe o es la misma entidad, actualiza el número
            entity.setReceiptNumber(dto.getReceiptNumber());
        } else if (dto.getReceiptNumber() != null) {
            // El número no cambió, no hacemos nada o lo reasignamos
            entity.setReceiptNumber(dto.getReceiptNumber()); // Ya lo tiene
        }

        if (dto.getTransactionType() != null && !dto.getTransactionType().equals(entity.getTransactionType())) {
            entity.setTransactionType(dto.getTransactionType());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getTransactionDate() != null) {
            entity.setTransactionDate(dto.getTransactionDate());
        }

        if (entity.getBankEntries() != null) {
        entity.getBankEntries().clear();
        } else {
            // Inicializar la lista si es nula
            entity.setBankEntries(new ArrayList<>());
        }

        // Agregar los nuevos movimientos contables desde el DTO
        if (dto.getBankEntries() != null && !dto.getBankEntries().isEmpty()) {
            for (BankEntryDTO entryDto : dto.getBankEntries()) {
                BankEntry newEntry = mapper.map(entryDto, BankEntry.class);
                newEntry.setTransactionBank(entity); // Enlace con la transacción
                if (newEntry.getDate() == null) { // Agregar la fecha si no existe
                    newEntry.setDate(LocalDateTime.now());
                }
                entity.getBankEntries().add(newEntry); // Agregar a la lista
            }
        }

        // Recalcular el monto total basado en los nuevos asientos
        BigDecimal newTotal = calculateTotalAmount(entity.getTransactionType(), entity.getBankEntries());
        entity.setTotalAmount(newTotal);

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
     * Obtener todas las transacciones (con filtros dinámicos) - CORREGIDO PARA LocalDate
     */
  /*  public List<TransactionBankDTO> listAll(Integer idCatalog, LocalDate dateFrom, LocalDate dateTo) {

        Specification<TransactionBank> spec = Specification.where(null);

        // Filtro de Código de Cuenta (sin cambios)
        if (idCatalog != null) {
            spec = spec.and(hasAccountCode(idCatalog));
        }

        // --- CORRECCIÓN EN FILTROS DE FECHA (LocalDate) ---
        if (dateFrom != null && dateTo != null) {
            // Caso 1: Rango (ambas fechas)
            // Simplemente usa las LocalDate directamente
            spec = spec.and(transactionDateAfter(dateFrom));     // >= dateFrom
            spec = spec.and(transactionDateBefore(dateTo));    // <= dateTo

        } else if (dateFrom != null) {
            // Caso 2: Solo una fecha (buscar solo ESE día)
            // Si la columna es LocalDate, solo necesitamos comparar igualdad.
            // O podemos usar >= dateFrom AND <= dateFrom
            spec = spec.and(transactionDateAfter(dateFrom));   // >= dateFrom
            spec = spec.and(transactionDateBefore(dateFrom));  // <= dateFrom (Efectivamente busca solo ESE día)

        } else if (dateTo != null) {
            // Caso 3: Solo fecha de fin
            spec = spec.and(transactionDateBefore(dateTo));   // <= dateTo
        }
        // ------------------------------------

        List<TransactionBank> results = repository.findAll(spec);

        return results.stream()
                .map(entity -> mapper.map(entity, TransactionBankDTO.class))
                .collect(Collectors.toList());
    }*/

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
     * Crea una especificación para encontrar transacciones DESPUÉS O IGUAL a una fecha.
     * Acepta LocalDate.
     */
    private Specification<TransactionBank> transactionDateAfter(LocalDate startDate) {
        return (root, query, cb) ->
            // Comparación >=
            cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate);
    }

    /**
     * Crea una especificación para encontrar transacciones ANTES O IGUAL a una fecha.
     * Acepta LocalDate.
     */
    private Specification<TransactionBank> transactionDateBefore(LocalDate endDate) {
        return (root, query, cb) ->
            // Comparación <=
            cb.lessThanOrEqualTo(root.get("transactionDate"), endDate);
    }
    public List<TransactionBankDTO> listAll() { // <-- Quita los parámetros de entrada aquí
        List<TransactionBank> results = repository.findAll(); // <-- findAll() sin argumentos trae todo
        return results.stream()
                .map(entity -> mapper.map(entity, TransactionBankDTO.class))
                .collect(Collectors.toList());
    }
}
