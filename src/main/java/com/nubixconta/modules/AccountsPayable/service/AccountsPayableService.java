package com.nubixconta.modules.AccountsPayable.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayablePurchaseResponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayableReponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsResponseDTO;
import com.nubixconta.modules.AccountsPayable.entity.AccountsPayable;
import com.nubixconta.modules.AccountsPayable.repository.AccountsPayableRepository;
import com.nubixconta.modules.AccountsPayable.repository.PaymentDetailsRepository;
import com.nubixconta.modules.purchases.dto.purchase.PurchaseForAccountsPayableDTO;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.security.TenantContext;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public List<AccountsPayablePurchaseResponseDTO> findAllAccountsPayablePurchaseResponseDTO() {
        return repository.findByCompanyId(getCompanyIdFromContext()).stream()
                .filter(ar -> ar.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountsPayablePurchaseResponseDTO> findFilteredAccountsPayablePurchaseResponseDTO(
            String supplierName, String documentNumber, LocalDate startDate, LocalDate endDate) {

        Integer companyId = getCompanyIdFromContext();
        return repository.findByCompanyId(companyId).stream()
                .filter(ar -> ar.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .filter(ar -> {
                    if (ar.getPurcharse() == null) {
                        return false;
                    }
                    Purchase purchase = ar.getPurcharse();

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
            PurchaseForAccountsPayableDTO purchaseDto = modelMapper.map(ar.getPurcharse(), PurchaseForAccountsPayableDTO.class);
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
                        PurchaseForAccountsPayableDTO purchaseDTO = new PurchaseForAccountsPayableDTO();
                        purchaseDTO.setDocumentNumber(account.getPurcharse().getDocumentNumber());
                        purchaseDTO.setIssueDate(account.getPurcharse().getIssueDate());
                        purchaseDTO.setTotalAmount(account.getPurcharse().getTotalAmount());

                        if (account.getPurcharse().getSupplier() != null) {
                            purchaseDTO.setSupplierName(account.getPurcharse().getSupplier().getSupplierName());
                            purchaseDTO.setSupplierLastName(account.getPurcharse().getSupplier().getSupplierLastName());
                            purchaseDTO.setCreditDay(account.getPurcharse().getSupplier().getCreditDay());
                        }

                        dto.setPurchase(purchaseDTO);
                    }
                    // Transformar manualmente los detalles de cobro por que ModelMapper
                    //no puede mapea todo el objeto CollectionDetail por defecto y no lo convierte
                    // a CollectionDetailTDO hay que hacerlo manualmente
                    List<PaymentDetailsResponseDTO> paymentDTOs = account.getPaymentDetails().stream()
                            .map(cd -> new PaymentDetailsResponseDTO(
                                    cd.getId(),
                                    cd.getPaymentStatus(),
                                    cd.getPaymentDetailDescription(),
                                    cd.getPaymentDetailsDate(),
                                    cd.getPaymentMethod(),
                                    cd.getPaymentAmount()
                            ))
                            .toList();

                    dto.setPaymentDetails(paymentDTOs);

                    return dto;
                })
                .collect(Collectors.toList());
    }
}
