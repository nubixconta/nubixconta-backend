package com.nubixconta.modules.AccountsPayable.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayablePurchaseResponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.AccountsPayable.AccountsPayableReponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsResponseDTO;
import com.nubixconta.modules.AccountsPayable.entity.AccountsPayable;
import com.nubixconta.modules.AccountsPayable.entity.PaymentDetails;
import com.nubixconta.modules.AccountsPayable.repository.AccountsPayableRepository;
import com.nubixconta.modules.AccountsPayable.repository.PaymentDetailsRepository;
import com.nubixconta.modules.accountsreceivable.dto.accountsreceivable.AccountsReceivableSaleResponseDTO;
import com.nubixconta.modules.purchases.dto.purchase.PurchaseForAccountsPayableDTO;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.modules.sales.dto.sales.SaleForAccountsReceivableDTO;
import com.nubixconta.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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

   /* @Transactional(readOnly = true)
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

        if (ar.getPurchase() != null) {
            PurchaseForAccountsPayableDTO purchaseDto = modelMapper.map(ar.getPurchase(), PurchaseForAccountsPayableDTO.class);
            if (ar.getPurchase().getSupplier() != null) {
                purchaseDto.setSupplierName(ar.getPurchase().getSupplier().getSupplierName());
                purchaseDto.setSupplierLastName(ar.getPurchase().getSupplier().getSupplierLastName());
                purchaseDto.setCreditDay(ar.getPurchase().getSupplier().getCreditDay());
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
}
