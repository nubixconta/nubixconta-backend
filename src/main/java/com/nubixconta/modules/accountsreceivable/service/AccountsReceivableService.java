package com.nubixconta.modules.accountsreceivable.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.accountsreceivable.dto.accountsreceivable.AccountsReceivableResponseDTO;
import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailResponseDTO;
import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.repository.AccountsReceivableRepository;
import com.nubixconta.modules.sales.dto.sales.SaleForAccountsReceivableDTO;
import com.nubixconta.security.TenantContext;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccountsReceivableService {

    private final AccountsReceivableRepository repository;
    private final ModelMapper modelMapper;

    public AccountsReceivableService(AccountsReceivableRepository repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
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
                                    cd.getPaymentMethod()
                            ))
                            .toList();

                    dto.setCollectionDetails(collectionDTOs);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Optional<AccountsReceivable> findById(Integer id) {
        Integer companyId = getCompanyIdFromContext();
        return repository.findByIdAndCompanyId(id, companyId); // <-- Usar el nuevo método del repositorio
    }

    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    public Optional<AccountsReceivable> findBySaleId(Integer saleId) {
        Integer companyId = getCompanyIdFromContext();
        return repository.findBySaleIdAndCompanyId(saleId, companyId); // <-- Usar el nuevo método del repositorio
    }

}
