package com.nubixconta.modules.AccountsPayable.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.AccountsPayable.entity.PaymentDetails;
import com.nubixconta.modules.AccountsPayable.repository.AccountsPayableRepository;
import com.nubixconta.modules.AccountsPayable.repository.PaymentDetailsRepository;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.security.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentDetailsService {

    private final PaymentDetailsRepository repository;
    //private final PurchaseRepo saleRepository;
    private ChangeHistoryService changeHistoryService;
    private final CompanyRepository companyRepository;

    @Autowired
    private AccountsPayableRepository accountsPayableRepository;

    public PaymentDetailsService(PaymentDetailsRepository repository,
                                 //SaleRepository saleRepository,
                                 ChangeHistoryService changeHistoryService,
                                 CompanyRepository companyRepository) {
        this.repository = repository;
        //this.saleRepository = saleRepository;
        this.changeHistoryService = changeHistoryService;
        this.companyRepository = companyRepository;
    }


    // Helper privado para obtener el contexto de la empresa de forma segura y consistente.
    public Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

    @Transactional
    public void deleteById(Integer id) {
        PaymentDetails detail = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el detalle con ID: " + id));

        //  Solo se puede eliminar si NO ha sido aplicado
        if ("APLICADO".equalsIgnoreCase(detail.getPaymentStatus())) {
            throw new IllegalStateException("No se puede eliminar un pago ya aplicado.");
        }

        Integer accountsPayableId = detail.getAccountsPayable().getId();

        // Primero eliminar
        repository.deleteById(id);

        // Luego recalcular el saldo
        // :TODO Pendiente recalcularBalancePorReceivableId(accountsPayableId);
    }

    public Optional<PaymentDetails> findById(Integer id) {
        return repository.findById(id);
    }


    public PaymentDetails save(PaymentDetails detail) {
        if (detail.getAccountsPayable() == null || detail.getAccountsPayable().getId() == null) {
            throw new IllegalArgumentException("Debe incluir el objeto accountsPayable con su id");
        }

        // Buscar la entidad completa y setearla
        Integer id = detail.getAccountsPayable().getId();
        var accountPayable = accountsPayableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe accountPayable con ID: " + id));
        detail.setAccountsPayable(accountPayable);
        return repository.save(detail);
    }
    @Transactional
    public void recalcularBalancePorPayableId(Integer payableId) {
        var ar = accountsPayableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("AccountsPayable no encontrado"));

        // Obtener solo los abonos que NO estén anulados
        Integer companyId = getCompanyIdFromContext();
        List<PaymentDetails> abonos = repository.findByAccountsPayableAndCompanyId(payableId,companyId).stream()
                .filter(detalle -> !"ANULADO".equalsIgnoreCase(detalle.getPaymentStatus()))
                .toList();

        BigDecimal totalAbonos = abonos.stream()
                .map(PaymentDetails::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Purchase purchase = ar.getPurcharse();
        if (purchase == null) {
            throw new IllegalStateException("La relación con la compra no está disponible para esta cuenta por pagar.");
        }

        BigDecimal montoTotalCompra = purchase.getTotalAmount();
        BigDecimal nuevoBalance = montoTotalCompra.subtract(totalAbonos);

        if (nuevoBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("El balance calculado es negativo, lo que indica un error en los datos.");
        }
        ar.setBalance(nuevoBalance);
        accountsPayableRepository.save(ar);
    }

}

