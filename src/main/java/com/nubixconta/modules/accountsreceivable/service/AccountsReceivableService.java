package com.nubixconta.modules.accountsreceivable.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.accountsreceivable.dto.accountsreceivable.AccountsReceivableResponseDTO;
import com.nubixconta.modules.accountsreceivable.dto.accountsreceivable.AccountsReceivableSaleResponseDTO;
import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailResponseDTO;
import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.repository.AccountsReceivableRepository;
import com.nubixconta.modules.accountsreceivable.repository.CollectionDetailRepository;
import com.nubixconta.modules.sales.dto.sales.SaleForAccountsReceivableDTO;
import com.nubixconta.security.TenantContext;
import org.springframework.transaction.annotation.Transactional; // <--- Añade esta línea
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccountsReceivableService {

    private final AccountsReceivableRepository repository;
    private final ModelMapper modelMapper;
    private final CollectionDetailRepository collectionDetailRepository;

    // Mapa estático para definir el orden numérico de los estados.
    private static final Map<String, Integer> STATUS_ORDER = Map.of(
            "PENDIENTE", 0,
            "APLICADO", 1,
            "ANULADO", 2
    );

    public AccountsReceivableService(AccountsReceivableRepository repository, ModelMapper modelMapper,CollectionDetailRepository collectionDetailRepository) {
        this.repository = repository;
        this.modelMapper = modelMapper;
        this.collectionDetailRepository = collectionDetailRepository;
    }
    // Helper privado para obtener el contexto de la empresa de forma segura y consistente.
    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

//Este metodo es para filtrar un cobro por un cliente y mostrar en una tabla el estado de cuenta por cliente
public List<Map<String, Serializable>> searchByCustomer(String name, String lastName, String dui, String nit) {
    Integer companyId = getCompanyIdFromContext();

    // 1. Inicializar la Specification con el filtro de la empresa.
    // Este filtro es obligatorio y siempre se aplicará.
    Specification<AccountsReceivable> spec = (root, query, cb) ->
            cb.equal(root.get("company").get("id"), companyId);

    // 2. Añadir los filtros opcionales del cliente a la Specification existente.
    // Cada filtro se une al anterior con un `AND`.
    if (name != null && !name.isBlank()) {
        Specification<AccountsReceivable> nameSpec = (root, query, cb) ->
                cb.like(cb.lower(root.get("sale").get("customer").get("customerName")), "%" + name.toLowerCase() + "%");
        spec = spec.and(nameSpec);
    }

    if (lastName != null && !lastName.isBlank()) {
        Specification<AccountsReceivable> lastNameSpec = (root, query, cb) ->
                cb.like(cb.lower(root.get("sale").get("customer").get("customerLastName")), "%" + lastName.toLowerCase() + "%");
        spec = spec.and(lastNameSpec);
    }

    if (dui != null && !dui.isBlank()) {
        Specification<AccountsReceivable> duiSpec = (root, query, cb) ->
                cb.equal(root.get("sale").get("customer").get("customerDui"), dui);
        spec = spec.and(duiSpec);
    }

    if (nit != null && !nit.isBlank()) {
        Specification<AccountsReceivable> nitSpec = (root, query, cb) ->
                cb.equal(root.get("sale").get("customer").get("customerNit"), nit);
        spec = spec.and(nitSpec);
    }

    // 3. Ejecutar la búsqueda con la Specification que ahora incluye el filtro de la empresa
    // y los filtros opcionales.
    LocalDate today = LocalDate.now();

    return repository.findAll(spec).stream()
            .map(account -> {
                var sale = account.getSale();
                var customer = sale.getCustomer();
                LocalDate issueDate = sale.getIssueDate().toLocalDate();
                LocalDate dueDate = issueDate.plusDays(customer.getCreditDay());
                long daysLate = today.isAfter(dueDate) ? ChronoUnit.DAYS.between(dueDate, today) : 0;

                Map<String, Serializable> data = new HashMap<>();
                data.put("documentNumber", sale.getDocumentNumber());
                data.put("customerName", customer.getCustomerName());
                data.put("customerLastName", customer.getCustomerLastName());
                data.put("issueDate", issueDate.toString());
                data.put("daysLate", daysLate);
                data.put("balance", account.getBalance());

                return data;
            })
            .collect(Collectors.toList());

}

    /**
     * MÉTODO ORIGINAL: Devuelve todos los registros sin un orden específico en los detalles
     */
    public List<AccountsReceivableResponseDTO> findAll() {
        Integer companyId = getCompanyIdFromContext();
        return repository.findByCompanyId(companyId).stream()
                .map(account -> {
                    AccountsReceivableResponseDTO dto = modelMapper.map(account, AccountsReceivableResponseDTO.class);

                    if (account.getSale() != null) {
                        SaleForAccountsReceivableDTO saleDTO = new SaleForAccountsReceivableDTO();
                        saleDTO.setDocumentNumber(account.getSale().getDocumentNumber());
                        saleDTO.setIssueDate(account.getSale().getIssueDate());
                        saleDTO.setTotalAmount(account.getSale().getTotalAmount());

                        if (account.getSale().getCustomer() != null) {
                            saleDTO.setCustomerName(account.getSale().getCustomer().getCustomerName());
                            saleDTO.setCustomerLastName(account.getSale().getCustomer().getCustomerLastName());
                            saleDTO.setCreditDay(account.getSale().getCustomer().getCreditDay());
                        }

                        dto.setSale(saleDTO);
                    }
                    // Transformar manualmente los detalles de cobro por que ModelMapper
                    //no puede mapea todo el objeto CollectionDetail por defecto y no lo convierte
                    // a CollectionDetailTDO hay que hacerlo manualmente
                    List<CollectionDetailResponseDTO> collectionDTOs = account.getCollectionDetails().stream()
                            .map(cd -> new CollectionDetailResponseDTO(
                                    cd.getId(),
                                    cd.getPaymentStatus(),
                                    cd.getPaymentDetailDescription(),
                                    cd.getCollectionDetailDate(),
                                    cd.getPaymentMethod(),
                                    cd.getPaymentAmount(),
                                    cd.getReference()
                            ))
                            .toList();

                    dto.setCollectionDetails(collectionDTOs);

                    return dto;
                })
                .collect(Collectors.toList());
    }
    /**
     *   Devuelve los datos con los detalles ordenados por fecha descendente.
     */
    public List<AccountsReceivableResponseDTO> findAllSortedByDate() {
        Integer companyId = getCompanyIdFromContext();
        List<AccountsReceivable> accounts = repository.findByCompanyId(companyId);

        // Define el comparador para ordenar los detalles de cobro por fecha (de más reciente a más antiguo)
        Comparator<CollectionDetailResponseDTO> detailDateComparator = Comparator
                .comparing(CollectionDetailResponseDTO::getCollectionDetailDate, Comparator.nullsLast(Comparator.reverseOrder()));

        // Define el comparador para ordenar la lista principal de cuentas por cobrar (por fecha de la venta)
        Comparator<AccountsReceivableResponseDTO> mainListComparator = Comparator
                .comparing(dto -> dto.getSale().getIssueDate(), Comparator.nullsLast(Comparator.reverseOrder()));

        return accounts.stream()
                // Primero, mapea a DTOs y ordena los detalles internos
                .map(account -> mapToDtoAndSortDetails(account, detailDateComparator))
                // Luego, ordena la lista principal de DTOs usando el nuevo comparador
                .sorted(mainListComparator)
                .collect(Collectors.toList());
    }


    /**
     *  Devuelve los datos con los detalles ordenados por estado personalizado.
     */
    /**
     * Devuelve los datos con los detalles ordenados por estado personalizado,
     * y la lista principal de cuentas por cobrar ordenada por el estado de su primer detalle.
     */
    public List<AccountsReceivableResponseDTO> findAllSortedByStatus() {
        Integer companyId = getCompanyIdFromContext();
        List<AccountsReceivable> accounts = repository.findByCompanyId(companyId);

        // 1. Define el comparador para ordenar los detalles de cada cuenta
        Comparator<CollectionDetailResponseDTO> statusComparator = Comparator
                .comparing(dto -> STATUS_ORDER.getOrDefault(dto.getPaymentStatus(), 99));

        // 2. Define el comparador para ordenar la lista principal de cuentas por cobrar
        Comparator<AccountsReceivableResponseDTO> mainListComparator = Comparator
                .comparing(dto -> {
                    // Obtiene el estado del primer detalle de cobro para ordenar la lista principal
                    return dto.getCollectionDetails().stream()
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
     * MÉTODO PRIVADO REUTILIZABLE: Contiene la lógica de mapeo y aplica un ordenamiento opcional.
     * @param account La entidad a convertir.
     * @param detailComparator El comparador para ordenar los detalles. Si es nulo, no se ordena.
     * @return El DTO procesado.
     */
    private AccountsReceivableResponseDTO mapToDtoAndSortDetails(AccountsReceivable account, Comparator<CollectionDetailResponseDTO> detailComparator) {
        // Mapeo inicial que ya tenías
        AccountsReceivableResponseDTO dto = modelMapper.map(account, AccountsReceivableResponseDTO.class);

        if (account.getSale() != null) {
            SaleForAccountsReceivableDTO saleDTO = new SaleForAccountsReceivableDTO();
            saleDTO.setDocumentNumber(account.getSale().getDocumentNumber());
            saleDTO.setIssueDate(account.getSale().getIssueDate());
            saleDTO.setTotalAmount(account.getSale().getTotalAmount());

            if (account.getSale().getCustomer() != null) {
                saleDTO.setCustomerName(account.getSale().getCustomer().getCustomerName());
                saleDTO.setCustomerLastName(account.getSale().getCustomer().getCustomerLastName());
                saleDTO.setCreditDay(account.getSale().getCustomer().getCreditDay());
            }
            dto.setSale(saleDTO);
        }

        // Mapeo manual de detalles que ya tenías
        List<CollectionDetailResponseDTO> collectionDTOs = account.getCollectionDetails().stream()
                .map(cd -> new CollectionDetailResponseDTO(
                        cd.getId(),
                        cd.getPaymentStatus(),
                        cd.getPaymentDetailDescription(),
                        cd.getCollectionDetailDate(),
                        cd.getPaymentMethod(),
                        cd.getPaymentAmount(),
                        cd.getReference()
                ))
                .collect(Collectors.toList()); // Usamos toList() para que sea mutable


        // Si se proporcionó un comparador, se usa para ordenar la lista de detalles.
        if (detailComparator != null) {
            collectionDTOs.sort(detailComparator);
        }

        dto.setCollectionDetails(collectionDTOs);
        return dto;
    }



    // ¡ MÉTODO PARA FILTRAR POR RANGO DE FECHAS!
    @Transactional(readOnly = true)
    public List<AccountsReceivableResponseDTO> findByCollectionDateRange(LocalDate startDate, LocalDate endDate) {
        Integer companyId = getCompanyIdFromContext();

        // Aseguramos que el rango cubra todo el día de fin
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 1. Obtenemos solo los detalles de cobro dentro del rango de fechas
        List<CollectionDetail> detailsInRange = collectionDetailRepository.findByCompanyIdAndCollectionDetailDateBetween(companyId, startDateTime, endDateTime);

        // 2. Procesamos los resultados para agrupar por cuenta por cobrar
        return detailsInRange.stream()
                // Mapeamos cada detalle a su cuenta por cobrar padre
                .map(CollectionDetail::getAccountReceivable)
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
    private AccountsReceivableResponseDTO convertToDtoWithFilteredDetails(AccountsReceivable account, LocalDateTime start, LocalDateTime end) {
        AccountsReceivableResponseDTO dto = new AccountsReceivableResponseDTO();
        dto.setBalance(account.getBalance());

        // Mapear la información de la venta (Sale)
        SaleForAccountsReceivableDTO saleDto = new SaleForAccountsReceivableDTO();
        if (account.getSale() != null) {
            saleDto.setDocumentNumber(account.getSale().getDocumentNumber());
            saleDto.setTotalAmount(account.getSale().getTotalAmount());
            saleDto.setIssueDate(account.getSale().getIssueDate());
            // Asumiendo que la entidad Sale tiene una relación con Customer
            if (account.getSale().getCustomer() != null) {
                saleDto.setCustomerName(account.getSale().getCustomer().getCustomerName());
                saleDto.setCustomerLastName(account.getSale().getCustomer().getCustomerLastName());
            }
            saleDto.setCreditDay(account.getSale().getCustomer().getCreditDay());
        }
        dto.setSale(saleDto);

        // 3. Filtrar los detalles de ESTA cuenta para que solo se incluyan los del rango
        List<CollectionDetailResponseDTO> filteredDetailsDto = account.getCollectionDetails().stream()
                .filter(detail ->
                        !detail.getCollectionDetailDate().isBefore(start) &&
                                !detail.getCollectionDetailDate().isAfter(end)
                )
                .map(this::convertDetailToDto) // Mapear cada detalle a su DTO
                .collect(Collectors.toList());

        dto.setCollectionDetails(filteredDetailsDto);
        dto.setCreditDay(account.getSale() != null ? account.getSale().getCustomer().getCreditDay() : null);

        return dto;
    }

    /**
     * Helper para convertir un CollectionDetail a CollectionDetailResponseDTO.
     */
    private CollectionDetailResponseDTO convertDetailToDto(CollectionDetail detail) {
        CollectionDetailResponseDTO dto = new CollectionDetailResponseDTO();
        dto.setId(detail.getId());
        dto.setPaymentStatus(detail.getPaymentStatus());
        dto.setPaymentDetailDescription(detail.getPaymentDetailDescription());
        dto.setPaymentMethod(detail.getPaymentMethod());
        dto.setPaymentAmount(detail.getPaymentAmount());
        dto.setReference(detail.getReference());
        dto.setCollectionDetailDate(detail.getCollectionDetailDate());
        // Agrega cualquier otro campo que tengas en CollectionDetailResponseDTO
        return dto;
    }

    @Transactional(readOnly = true)
    public List<AccountsReceivableSaleResponseDTO> findAllAccountsReceivableSaleResponseDTO() {
        Integer companyId = getCompanyIdFromContext();
        return repository.findByCompanyId(companyId).stream()
                .filter(ar -> ar.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(ar -> {
                    AccountsReceivableSaleResponseDTO dto = new AccountsReceivableSaleResponseDTO();
                    dto.setBalance(ar.getBalance());

                    if (ar.getSale() != null) {
                        SaleForAccountsReceivableDTO saleDto = modelMapper.map(ar.getSale(), SaleForAccountsReceivableDTO.class);
                        if (ar.getSale().getCustomer() != null) {
                            saleDto.setCustomerName(ar.getSale().getCustomer().getCustomerName());
                            saleDto.setCustomerLastName(ar.getSale().getCustomer().getCustomerLastName());
                        }
                        saleDto.setCreditDay(ar.getSale().getCustomer().getCreditDay());
                        dto.setSale(saleDto);
                    } else {
                        dto.setSale(null); // Ensure sale is null if not present
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Valida si una venta tiene cobros asociados en estado "APLICADO" o "PENDIENTE".
     * Devuelve `true` si la venta tiene al menos un cobro asociado con dichos estados,
     * `false` en caso contrario.
     * @param saleId El ID de la venta a validar.
     * @return `true` si la venta tiene cobros en estado "APLICADO" o "PENDIENTE", `false` si no los tiene.
     */
    @Transactional(readOnly = true) // Añadir Transactional para asegurar que la colección se carga
    public boolean validateSaleWithoutCollections(Integer saleId) {
        Integer companyId = getCompanyIdFromContext();

        Optional<AccountsReceivable> accountsReceivableOptional = repository.findBySale_SaleIdAndCompany_IdWithCollectionDetails(saleId, companyId);

        if (accountsReceivableOptional.isPresent()) {
            AccountsReceivable ar = accountsReceivableOptional.get();
            // Filtrar los collectionDetails para contar solo los que están en estado "APLICADO" o "PENDIENTE"
            long activeCollections = ar.getCollectionDetails().stream()
                    .filter(cd -> "APLICADO".equalsIgnoreCase(cd.getPaymentStatus()) ||
                            "PENDIENTE".equalsIgnoreCase(cd.getPaymentStatus()))
                    .count();

            // Si hay al menos un cobro en estado "APLICADO" o "PENDIENTE", devuelve true
            return activeCollections > 0;
        } else {
            // Si no hay AccountsReceivable asociada a la venta, significa que no hay cobros.
            return false;
        }
    }


    public Optional<AccountsReceivable> findById(Integer id) {
        Integer companyId = getCompanyIdFromContext();
        return repository.findByIdAndCompanyId(id, companyId); // <-- Usar el nuevo método del repositorio
    }



    // Método de conversión a DTO (puede que ya lo tengas)

    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    public Optional<AccountsReceivable> findBySaleId(Integer saleId) {
        Integer companyId = getCompanyIdFromContext();
        return repository.findBySaleIdAndCompanyId(saleId, companyId); // <-- Usar el nuevo método del repositorio
    }

}
