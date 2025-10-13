package com.nubixconta.modules.AccountsPayable.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayablePurchaseResponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayableReponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsResponseDTO;
import com.nubixconta.modules.AccountsPayable.entity.AccountsPayable;
import com.nubixconta.modules.AccountsPayable.entity.PaymentDetails;
import com.nubixconta.modules.AccountsPayable.repository.AccountsPayableRepository;
import com.nubixconta.modules.AccountsPayable.repository.PaymentDetailsRepository;
import com.nubixconta.modules.purchases.dto.purchases.PurchaseDetailResponseDTO;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccountsPayableService {

    private final AccountsPayableRepository repository;
    private final ModelMapper modelMapper;
    private final PaymentDetailsRepository paymentDetailsRepository;

    // Mapa estático para definir el orden numérico de los estados.
    private static final Map<String, Integer> STATUS_ORDER = Map.of(
            "PENDIENTE", 0,
            "APLICADO", 1,
            "ANULADO", 2
    );

    public AccountsPayableService(AccountsPayableRepository repository, ModelMapper modelMapper,PaymentDetailsRepository paymentDetailsRepository) {
        this.repository = repository;
        this.modelMapper = modelMapper;
        this.paymentDetailsRepository = paymentDetailsRepository;


    }


    // Helper privado para obtener el contexto de la empresa de forma segura y consistente.
    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }
/*
    @Transactional(readOnly = true)
    public List<AccountsPayablePurchaseResponseDTO> findAllAccountsPayablePurchaseResponseDTO() {
        return repository.findByCompanyId(getCompanyIdFromContext()).stream()
                .filter(ar -> ar.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }*/

    @Transactional(readOnly = true)
    public List<AccountsPayablePurchaseResponseDTO> findAllAccountsPayablePurchaseResponseDTO() {
        Integer companyId = getCompanyIdFromContext();
        return repository.findByCompanyId(companyId).stream()
                .filter(ar -> ar.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(ar -> {
                    AccountsPayablePurchaseResponseDTO dto = new AccountsPayablePurchaseResponseDTO();
                    dto.setBalance(ar.getBalance());

                    if (ar.getPurchase() != null) {
                        Purchase purchase = ar.getPurchase();
                        PurchaseForAccountsPayableDTO purchaseDto = new PurchaseForAccountsPayableDTO();

                        // Mapeo manual de campos de la compra
                        purchaseDto.setDocumentNumber(purchase.getDocumentNumber());
                        purchaseDto.setIdPurchase(purchase.getIdPurchase());
                        purchaseDto.setTotalAmount(purchase.getTotalAmount());
                        purchaseDto.setIssueDate(purchase.getIssueDate());
                        purchaseDto.setPurchaseDescription(purchase.getPurchaseDescription());

                        // Mapeo manual de campos del proveedor
                        if (purchase.getSupplier() != null) {
                            purchaseDto.setSupplierName(purchase.getSupplier().getSupplierName());
                            purchaseDto.setSupplierLastName(purchase.getSupplier().getSupplierLastName());
                            purchaseDto.setCreditDay(purchase.getSupplier().getCreditDay());
                        }

                        dto.setPurchase(purchaseDto);
                    } else {
                        dto.setPurchase(null);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<AccountsPayablePurchaseResponseDTO> findFilteredAccountsPayablePurchaseResponseDTO(
            String supplierName, String documentNumber, LocalDate startDate, LocalDate endDate) {

        Integer companyId = getCompanyIdFromContext();
        return repository.findByCompanyId(companyId).stream()
                .filter(ar -> ar.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .filter(ar -> {
                    if (ar.getPurchase() == null) {
                        return false;
                    }
                    Purchase purchase = ar.getPurchase();

                    // Convierte LocalDateTime a LocalDate para la comparación
                    LocalDate purchaseIssueDate = purchase.getIssueDate().toLocalDate();

                    boolean supplierMatch = (supplierName == null ||
                            (purchase.getSupplier() != null &&
                                    purchase.getSupplier().getSupplierName().equalsIgnoreCase(supplierName)));

                    boolean documentMatch = (documentNumber == null || purchase.getDocumentNumber().equalsIgnoreCase(documentNumber));

                    boolean dateMatch = true;
                    if (startDate != null && endDate != null) {
                        dateMatch = !purchaseIssueDate.isBefore(startDate) && !purchaseIssueDate.isAfter(endDate);
                    } else if (startDate != null) {
                        dateMatch = !purchaseIssueDate.isBefore(startDate);
                    } else if (endDate != null) {
                        dateMatch = !purchaseIssueDate.isAfter(endDate);
                    }

                    return supplierMatch && documentMatch && dateMatch;
                })
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Helper privado para evitar código duplicado
    private AccountsPayablePurchaseResponseDTO convertToDto(AccountsPayable ar) {
        AccountsPayablePurchaseResponseDTO dto = new AccountsPayablePurchaseResponseDTO();
        dto.setBalance(ar.getBalance());

        if (ar.getPurcharse() != null) {
            PurchaseDetailResponseDTO.PurchaseForAccountsPayableDTO purchaseDto = modelMapper.map(ar.getPurcharse(), PurchaseDetailResponseDTO.PurchaseForAccountsPayableDTO.class);
            if (ar.getPurcharse().getSupplier() != null) {
                purchaseDto.setSupplierName(ar.getPurcharse().getSupplier().getSupplierName());
                purchaseDto.setSupplierLastName(ar.getPurcharse().getSupplier().getSupplierLastName());
                purchaseDto.setCreditDay(ar.getPurcharse().getSupplier().getCreditDay());
            }
            dto.setPurchase(purchaseDto);
        } else {
            dto.setPurchase(null);
        }
        return dto;
    }


    /**
     * MÉTODO ORIGINAL: Devuelve todos los registros sin un orden específico en los detalles
     */
    public List<AccountsPayableReponseDTO> findAll() {
        Integer companyId = getCompanyIdFromContext();
        return repository.findByCompanyId(companyId).stream()
                .map(account -> {
                    AccountsPayableReponseDTO dto = modelMapper.map(account, AccountsPayableReponseDTO.class);

                    if (account.getPurcharse() != null) {
                        PurchaseDetailResponseDTO.PurchaseForAccountsPayableDTO purchaseDTO = new PurchaseDetailResponseDTO.PurchaseForAccountsPayableDTO();
                        purchaseDTO.setDocumentNumber(account.getPurcharse().getDocumentNumber());
                        purchaseDTO.setIssueDate(account.getPurcharse().getIssueDate());
                        purchaseDTO.setTotalAmount(account.getPurcharse().getTotalAmount());

                        if (account.getPurcharse().getSupplier() != null) {
                            purchaseDTO.setSupplierName(account.getPurcharse().getSupplier().getSupplierName());
                            purchaseDTO.setSupplierLastName(account.getPurcharse().getSupplier().getSupplierLastName());
                            purchaseDTO.setCreditDay(account.getPurcharse().getSupplier().getCreditDay());
                        }

        // *** 1. USAMOS EL NUEVO MÉTODO DEL REPOSITORIO ***
        // Esto asegura que la lista 'paymentDetails' se cargue (resuelve el Lazy Loading).
        List<AccountsPayable> accounts = repository.findByCompanyIdWithDetails(companyId);

        return accounts.stream()
                .map(this::mapEntityToDTO) // Delegamos el mapeo a un método dedicado
                .collect(Collectors.toList());
    }


    private AccountsPayableReponseDTO mapEntityToDTO(AccountsPayable account) {
        AccountsPayableReponseDTO dto = new AccountsPayableReponseDTO();

        // 1. Mapeo de propiedades directas
        dto.setBalance(account.getBalance());
        dto.setPayableAmount(account.getPayableAmount());

        // 2. Mapeo de Compra (Purchase)
        if (account.getPurchase() != null) {
            PurchaseForAccountsPayableDTO purchaseDTO = new PurchaseForAccountsPayableDTO();
            purchaseDTO.setDocumentNumber(account.getPurchase().getDocumentNumber());
            purchaseDTO.setIssueDate(account.getPurchase().getIssueDate());
            purchaseDTO.setTotalAmount(account.getPurchase().getTotalAmount());

            if (account.getPurchase().getSupplier() != null) {
                // Mapeo explícito que resuelve el conflicto de ModelMapper
                purchaseDTO.setSupplierName(account.getPurchase().getSupplier().getSupplierName());
                purchaseDTO.setSupplierLastName(account.getPurchase().getSupplier().getSupplierLastName());
                purchaseDTO.setCreditDay(account.getPurchase().getSupplier().getCreditDay());
            }

            dto.setPurchase(purchaseDTO);
        }

        // 3. Mapeo de Detalles de Pago (PaymentDetails)
        if (account.getPaymentDetails() != null) {
            List<PaymentDetailsResponseDTO> detailsDTO = account.getPaymentDetails().stream()
                    .map(this::mapPaymentDetailToDTO)
                    .collect(Collectors.toList());

            dto.setPaymentDetails(detailsDTO);
        } else {
            dto.setPaymentDetails(Collections.emptyList());
        }

        return dto;
    }

    private PaymentDetailsResponseDTO mapPaymentDetailToDTO(PaymentDetails detail) {
        PaymentDetailsResponseDTO dto = new PaymentDetailsResponseDTO();

        // Mapeo de las propiedades de PaymentDetails a PaymentDetailsResponseDTO
        dto.setId(detail.getId());
        dto.setPaymentAmount(detail.getPaymentAmount());
        dto.setPaymentMethod(detail.getPaymentMethod());
        dto.setPaymentStatus(detail.getPaymentStatus());
        dto.setPaymentDetailsDate(detail.getPaymentDetailsDate());
        dto.setPaymentDetailDescription(detail.getPaymentDetailDescription());

        return dto;
    }

    public AccountsPayable findOrCreateAccountsPayable(Purchase purchase) {
        // Se busca por el ID de la venta y el ID de la compañía para asegurar la pertenencia de los datos.
        return repository.findByPurchaseIdAndCompanyId(purchase.getIdPurchase(), purchase.getCompany().getId())
                .orElseGet(() -> {
                    // Si no existe, se crea una nueva instancia.
                    AccountsPayable newAR = new AccountsPayable();
                    newAR.setPurchaseId(purchase.getIdPurchase());
                    newAR.setPurchase(purchase);
                    newAR.setPayableAmount(purchase.getTotalAmount());// El monto del pago inicial es igual al monto total de la venta
                    newAR.setBalance(newAR.getPayableAmount());
                    newAR.setModuleType("Cuentas por pagar");
                    newAR.setCompany(purchase.getCompany());
                    return repository.save(newAR);
                });
    }

    @Transactional
    public AccountsPayable UpdatePayableAmountAndBalance(Integer purchaseId, BigDecimal amountToDecrease) {
        // 1. Obtener el ID de la compañía para asegurar la integridad de los datos.
        Integer companyId = getCompanyIdFromContext();

        // 2. Buscar la entidad AccountsPayable por el ID de la compra y de la compañía.
        AccountsPayable accountsPayable = repository.findByPurchaseIdAndCompanyId(purchaseId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la cuenta por pagar para el ID de compra y compañía proporcionados."));

        // 3. Validar el monto a disminuir.
        if (amountToDecrease == null || amountToDecrease.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El monto a disminuir debe ser un valor positivo.");
        }

        // 4. Validar que el monto a disminuir no exceda el PayableAmount y el Balance.
        if (amountToDecrease.compareTo(accountsPayable.getPayableAmount()) > 0 || amountToDecrease.compareTo(accountsPayable.getBalance()) > 0) {
            throw new BusinessRuleException("El monto a disminuir no puede ser mayor que el monto original a pagar o el saldo actual.");
        }

        // 5. Disminuir el PayableAmount y el Balance.
        BigDecimal newPayableAmount = accountsPayable.getPayableAmount().subtract(amountToDecrease);
        BigDecimal newBalance = accountsPayable.getBalance().subtract(amountToDecrease);

        accountsPayable.setPayableAmount(newPayableAmount);
        accountsPayable.setBalance(newBalance);

        // 6. Guardar los cambios en la base de datos.
        return repository.save(accountsPayable);
    }
    public List<AccountsPayableReponseDTO> findAllSortedByStatus() {
        Integer companyId = getCompanyIdFromContext();
        List<AccountsPayable> accounts = repository.findByCompanyId(companyId);

        // 1. Define el comparador para ordenar los detalles de cada cuenta
        Comparator<PaymentDetailsResponseDTO> statusComparator = Comparator
                .comparing(dto -> STATUS_ORDER.getOrDefault(dto.getPaymentStatus(), 99));

        // 2. Define el comparador para ordenar la lista principal de cuentas por cobrar
        Comparator<AccountsPayableReponseDTO> mainListComparator = Comparator
                .comparing(dto -> {
                    // Obtiene el estado del primer detalle de cobro para ordenar la lista principal
                    return dto.getPaymentDetails().stream()
                            .findFirst()
                            .map(detail -> STATUS_ORDER.getOrDefault(detail.getPaymentStatus(), 99))
                            .orElse(99); // Cuentas sin detalles van al final
                });

        return accounts.stream()
                // Mapea las entidades a DTOs y ordena los detalles internos
                .map(account -> mapToDtoAndSortDetails(account, statusComparator))
                // Ordena la lista principal de DTOs usando el estado del primer detalle
                .sorted(mainListComparator)
                .collect(Collectors.toList());
    }
    /**
     *   Devuelve los datos con los detalles ordenados por fecha descendente.
     */
    public List<AccountsPayableReponseDTO> findAllSortedByDate() {
        Integer companyId = getCompanyIdFromContext();
        List<AccountsPayable> accounts = repository.findByCompanyId(companyId);

        // Define el comparador para ordenar los detalles de cobro por fecha (de más reciente a más antiguo)
        Comparator<PaymentDetailsResponseDTO> detailDateComparator = Comparator
                .comparing(PaymentDetailsResponseDTO::getPaymentDetailsDate, Comparator.nullsLast(Comparator.reverseOrder()));

        // Define el comparador para ordenar la lista principal de cuentas por cobrar (por fecha de la venta)
        Comparator<AccountsPayableReponseDTO> mainListComparator = Comparator
                .comparing(dto -> dto.getPurchase().getIssueDate(), Comparator.nullsLast(Comparator.reverseOrder()));

        return accounts.stream()
                // Primero, mapea a DTOs y ordena los detalles internos
                .map(account -> mapToDtoAndSortDetails(account, detailDateComparator))
                // Luego, ordena la lista principal de DTOs usando el nuevo comparador
                .sorted(mainListComparator)
                .collect(Collectors.toList());
    }

    private AccountsPayableReponseDTO mapToDtoAndSortDetails(AccountsPayable account, Comparator<PaymentDetailsResponseDTO> detailComparator) {
        AccountsPayableReponseDTO dto = new AccountsPayableReponseDTO();
        dto.setBalance(account.getBalance());
        dto.setPayableAmount(account.getPayableAmount()); // Asegúrate de mapear esto también

        if (account.getPurchase() != null) {
            PurchaseForAccountsPayableDTO purchaseDTO = new PurchaseForAccountsPayableDTO();
            purchaseDTO.setDocumentNumber(account.getPurchase().getDocumentNumber());
            purchaseDTO.setIssueDate(account.getPurchase().getIssueDate());
            purchaseDTO.setTotalAmount(account.getPurchase().getTotalAmount());
            purchaseDTO.setIdPurchase(account.getPurchase().getIdPurchase()); // Asegúrate de mapear el ID de la compra
            purchaseDTO.setPurchaseDescription(account.getPurchase().getPurchaseDescription()); // Y la descripción

            if (account.getPurchase().getSupplier() != null) {
                // Mapeo manual para evitar el conflicto con ModelMapper
                purchaseDTO.setSupplierName(account.getPurchase().getSupplier().getSupplierName());
                purchaseDTO.setSupplierLastName(account.getPurchase().getSupplier().getSupplierLastName());
                purchaseDTO.setCreditDay(account.getPurchase().getSupplier().getCreditDay());
            }
            dto.setPurchase(purchaseDTO);
        }

        // Mapeo manual de detalles que ya tenías
        List<PaymentDetailsResponseDTO> paymentDTOs = account.getPaymentDetails().stream()
                .map(cd -> new PaymentDetailsResponseDTO(
                        cd.getId(),
                        cd.getPaymentStatus(),
                        cd.getPaymentDetailDescription(),
                        cd.getPaymentDetailsDate(),
                        cd.getPaymentMethod(),
                        cd.getPaymentAmount(),
                        cd.getReference()
                ))
                .collect(Collectors.toList()); // Usamos toList() para que sea mutable


        // Si se proporcionó un comparador, se usa para ordenar la lista de detalles.
        if (detailComparator != null) {
            paymentDTOs.sort(detailComparator);
        }

        dto.setPaymentDetails(paymentDTOs);
        dto.setCreditDay(account.getPurchase() != null && account.getPurchase().getSupplier() != null ? account.getPurchase().getSupplier().getCreditDay() : null); // Mapear CreditDay
        return dto;
    }
    // ¡ MÉTODO PARA FILTRAR POR RANGO DE FECHAS!
    @Transactional(readOnly = true)
    public List<AccountsPayableReponseDTO> findByPaymentDateRange(LocalDate startDate, LocalDate endDate) {
        Integer companyId = getCompanyIdFromContext();

        // Aseguramos que el rango cubra todo el día de fin
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 1. Obtenemos solo los detalles de cobro dentro del rango de fechas
        List<PaymentDetails> detailsInRange = paymentDetailsRepository.findByCompanyIdAndPaymentDetailsDateBetween(companyId, startDateTime, endDateTime);

        // 2. Procesamos los resultados para agrupar por cuenta por cobrar
        return detailsInRange.stream()
                // Mapeamos cada detalle a su cuenta por cobrar padre
                .map(PaymentDetails::getAccountsPayable)
                // Eliminamos duplicados para tener una lista única de cuentas por cobrar
                .distinct()
                // Convertimos cada cuenta por cobrar a su DTO
                .map(account -> convertToDtoWithFilteredDetails(account, startDateTime, endDateTime))
                .collect(Collectors.toList());
    }
    /**
     * Convierte una entidad AccountsReceivable a su DTO, pero asegurándose de que la lista
     * de collectionDetails SOLO contenga aquellos dentro del rango de fechas especificado.
     */
    private AccountsPayableReponseDTO convertToDtoWithFilteredDetails(AccountsPayable account, LocalDateTime start, LocalDateTime end) {
        AccountsPayableReponseDTO dto = new AccountsPayableReponseDTO();
        dto.setBalance(account.getBalance());
        dto.setPayableAmount(account.getPayableAmount()); // Asegúrate de mapear esto también

        // Mapear la información de la compra (Purchase)
        PurchaseForAccountsPayableDTO purchaseDto = new PurchaseForAccountsPayableDTO();
        if (account.getPurchase() != null) {
            purchaseDto.setDocumentNumber(account.getPurchase().getDocumentNumber());
            purchaseDto.setTotalAmount(account.getPurchase().getTotalAmount());
            purchaseDto.setIssueDate(account.getPurchase().getIssueDate());
            purchaseDto.setIdPurchase(account.getPurchase().getIdPurchase()); // Asegúrate de mapear el ID de la compra
            purchaseDto.setPurchaseDescription(account.getPurchase().getPurchaseDescription()); // Y la descripción

            // Asumiendo que la entidad Purchase tiene una relación con Supplier
            if (account.getPurchase().getSupplier() != null) {
                // Mapeo manual para evitar el conflicto con ModelMapper
                purchaseDto.setSupplierName(account.getPurchase().getSupplier().getSupplierName());
                purchaseDto.setSupplierLastName(account.getPurchase().getSupplier().getSupplierLastName());
                purchaseDto.setCreditDay(account.getPurchase().getSupplier().getCreditDay());
            }
        }
        dto.setPurchase(purchaseDto);

        // 3. Filtrar los detalles de ESTA cuenta para que solo se incluyan los del rango
        List<PaymentDetailsResponseDTO> filteredDetailsDto = account.getPaymentDetails().stream()
                .filter(detail ->
                        !detail.getPaymentDetailsDate().isBefore(start) &&
                                !detail.getPaymentDetailsDate().isAfter(end)
                )
                .map(this::convertDetailToDto) // Mapear cada detalle a su DTO
                .collect(Collectors.toList());

        dto.setPaymentDetails(filteredDetailsDto);
        // Asegúrate de manejar el caso donde purchase o supplier es nulo antes de intentar obtener CreditDay
        dto.setCreditDay(account.getPurchase() != null && account.getPurchase().getSupplier() != null ? account.getPurchase().getSupplier().getCreditDay() : null);

        return dto;
    }
    /**
     * Helper para convertir un CollectionDetail a CollectionDetailResponseDTO.
     */
    private PaymentDetailsResponseDTO convertDetailToDto(PaymentDetails detail) {
        PaymentDetailsResponseDTO dto = new PaymentDetailsResponseDTO();
        dto.setId(detail.getId());
        dto.setPaymentStatus(detail.getPaymentStatus());
        dto.setPaymentDetailDescription(detail.getPaymentDetailDescription());
        dto.setPaymentMethod(detail.getPaymentMethod());
        dto.setPaymentAmount(detail.getPaymentAmount());
        dto.setReference(detail.getReference());
        dto.setPaymentDetailsDate(detail.getPaymentDetailsDate());
        // Agrega cualquier otro campo que tengas en CollectionDetailResponseDTO
        return dto;
    }


    //Este metodo es para filtrar un pago por un proveedor y mostrar en una tabla el estado de cuenta por proveedor
    public List<Map<String, Serializable>> searchBySupplier(String name, String lastName, String dui, String nit) {
        Integer companyId = getCompanyIdFromContext();

        // 1. Inicializar la Specification con el filtro de la empresa.
        // Este filtro es obligatorio y siempre se aplicará.
        Specification<AccountsPayable> spec = (root, query, cb) ->
                cb.equal(root.get("company").get("id"), companyId);

        // 2. Añadir los filtros opcionales del cliente a la Specification existente.
        // Cada filtro se une al anterior con un `AND`.
        if (name != null && !name.isBlank()) {
            Specification<AccountsPayable> nameSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("purchase").get("supplier").get("supplierName")), "%" + name.toLowerCase() + "%");
            spec = spec.and(nameSpec);
        }

        if (lastName != null && !lastName.isBlank()) {
            Specification<AccountsPayable> lastNameSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("purchase").get("supplier").get("supplierLastName")), "%" + lastName.toLowerCase() + "%");
            spec = spec.and(lastNameSpec);
        }

        if (dui != null && !dui.isBlank()) {
            Specification<AccountsPayable> duiSpec = (root, query, cb) ->
                    cb.equal(root.get("purchase").get("supplier").get("supplierDui"), dui);
            spec = spec.and(duiSpec);
        }

        if (nit != null && !nit.isBlank()) {
            Specification<AccountsPayable> nitSpec = (root, query, cb) ->
                    cb.equal(root.get("purchase").get("supplier").get("supplierNit"), nit);
            spec = spec.and(nitSpec);
        }

        // 3. Ejecutar la búsqueda con la Specification que ahora incluye el filtro de la empresa
        // y los filtros opcionales.
        LocalDate today = LocalDate.now();

        return repository.findAll(spec).stream()
                .map(account -> {
                    var sale = account.getPurchase();
                    var customer = sale.getSupplier();
                    LocalDate issueDate = sale.getIssueDate().toLocalDate();
                    LocalDate dueDate = issueDate.plusDays(customer.getCreditDay());
                    long daysLate = today.isAfter(dueDate) ? ChronoUnit.DAYS.between(dueDate, today) : 0;

                    Map<String, Serializable> data = new HashMap<>();
                    data.put("documentNumber", sale.getDocumentNumber());
                    data.put("customerName", customer.getSupplierName());
                    data.put("customerLastName", customer.getSupplierLastName());
                    data.put("issueDate", issueDate.toString());
                    data.put("daysLate", daysLate);
                    data.put("balance", account.getBalance());

                    return data;
                })
                .collect(Collectors.toList());

    }
}
