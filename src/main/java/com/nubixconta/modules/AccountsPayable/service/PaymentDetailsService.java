package com.nubixconta.modules.AccountsPayable.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsCreateDTO;
import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsResponseDTO;
import com.nubixconta.modules.AccountsPayable.entity.AccountsPayable;
import com.nubixconta.modules.AccountsPayable.entity.PaymentDetails;
import com.nubixconta.modules.AccountsPayable.repository.AccountsPayableRepository;
import com.nubixconta.modules.AccountsPayable.repository.PaymentDetailsRepository;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.modules.purchases.entity.PurchaseCreditNote;
import com.nubixconta.modules.purchases.repository.PurchaseRepository;
import com.nubixconta.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaymentDetailsService {

    private final PaymentDetailsRepository repository;
    private final PurchaseRepository purchaseRepository;
    private final ModelMapper modelMapper;
    private ChangeHistoryService changeHistoryService;
    private final CompanyRepository companyRepository;

    @Autowired
    private AccountsPayableRepository accountsPayableRepository;

    public PaymentDetailsService(PaymentDetailsRepository repository,
                                 PurchaseRepository purchaseRepository,
                                 ChangeHistoryService changeHistoryService,
                                 CompanyRepository companyRepository,
                                 ModelMapper modelMapper
    ) {
        this.repository = repository;
        this.purchaseRepository = purchaseRepository;
        this.changeHistoryService = changeHistoryService;
        this.modelMapper = modelMapper;
        this.companyRepository = companyRepository;
    }


    // Helper privado para obtener el contexto de la empresa de forma segura y consistente.
    public Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

    public List<PaymentDetailsResponseDTO> findAll() {
        // La llamada a findAll() es suficiente. El filtro de Hibernate se encarga de
        // filtrar los resultados por la compañía del contexto.
        return repository.findAll().stream()
                .map(paymentDetail -> modelMapper.map(paymentDetail, PaymentDetailsResponseDTO.class))
                .collect(Collectors.toList());
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
        recalcularBalancePorPayableId(accountsPayableId);
    }

    public Optional<PaymentDetails> findById(Integer id) {
        return repository.findById(id);
    }

    //Metodo para filtrar por fechas
    public List<PaymentDetails> findByDateRange(LocalDateTime start, LocalDateTime end) {
        Integer companyId = getCompanyIdFromContext();
        // Llamar al nuevo método del repositorio.
        return repository.findByCompanyIdAndPaymentDetailsDateBetween(companyId, start, end);
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

        Purchase purchase = ar.getPurchase();
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


    @Transactional
    public PaymentDetails makePayment(PaymentDetailsCreateDTO dto) {
        Integer purchaseId = dto.getIdPurchase();
        Integer companyId = getCompanyIdFromContext();

        Purchase purchase = purchaseRepository.findByIdPurchaseAndCompanyId(purchaseId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Compra no encontrada o no pertenece a la empresa actual."));


        // **Llamada al nuevo método reutilizable**
        AccountsPayable ar = accountsPayableRepository.findByPurchase(purchase)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta por cobrar no encontrada para la venta. Asegúrese de que la venta haya sido aplicada."));


        BigDecimal montoTotalCompra = ar.getPurchase().getTotalAmount();
        BigDecimal saldoActual = ar.getBalance();
        BigDecimal abonoNuevo = dto.getPaymentAmount();
        BigDecimal nuevoSaldo = saldoActual.subtract(abonoNuevo);

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto a abonar excede el saldo restante de la compra.");
        }
        // Actualizar el balance
        ar.setBalance(nuevoSaldo);
        accountsPayableRepository.save(ar);

        //Obtener la referencia a la empresa.
        Company companyRef = companyRepository.getReferenceById(companyId);

        // Crear PaymentDetails desde DTO
        PaymentDetails detail = new PaymentDetails();
        detail.setAccountsPayable(ar);
        detail.setCompany(companyRef);
        detail.setAccountId(dto.getAccountId());
        detail.setReference(dto.getReference());
        detail.setPaymentMethod(dto.getPaymentMethod());
        detail.setPaymentStatus(dto.getPaymentStatus());
        detail.setPaymentAmount(dto.getPaymentAmount());
        detail.setPaymentDetailDescription(dto.getPaymentDetailDescription());
        detail.setPaymentDetailsDate(dto.getPaymentDetailsDate());
        detail.setModuleType(dto.getModuleType());

        PaymentDetails saved = repository.save(detail);
        recalcularBalancePorPayableId(ar.getId());

        // Bitácora de cambios
        changeHistoryService.logChange(
                "Cuentas por pagar",
                "Se realizo un pago para el numero de documento " + ar.getPurchase().getDocumentNumber()
                        + "A la empresa " + companyRef.getCompanyName()
        );
        return saved;
    }

    // =================================================================================
    // MÉTODOS PARA INTEGRACIÓN CON NOTAS DE CRÉDITO SOBRE COMPRAS (NUEVOS)
    // =================================================================================

    /**
     * Crea un abono (PaymentDetail) a partir de una nota de crédito de compra aplicada.
     * Este método es llamado por el orquestador de notas de crédito.
     * @param creditNote La nota de crédito que se está aplicando.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createPaymentFromCreditNote(PurchaseCreditNote creditNote) {
        AccountsPayable payableAccount = accountsPayableRepository.findByPurchase(creditNote.getPurchase())
                .orElseThrow(() -> new NotFoundException("No se encontró la cuenta por pagar asociada a la compra original de la NC."));

        // Creamos un nuevo detalle de pago que representa la nota de crédito.
        PaymentDetails creditNotePayment = new PaymentDetails();
        creditNotePayment.setAccountsPayable(payableAccount);
        creditNotePayment.setCompany(creditNote.getCompany());
        creditNotePayment.setPaymentAmount(creditNote.getTotalAmount());
        creditNotePayment.setPaymentMethod("NOTA_DE_CREDITO"); // Método de pago especial para trazabilidad.
        creditNotePayment.setPaymentStatus("APLICADO"); // Nace aplicado.
        creditNotePayment.setPaymentDetailsDate(creditNote.getIssueDate()); // Usa la fecha de la NC.
        creditNotePayment.setPaymentDetailDescription("Abono por Nota de Crédito N°: " + creditNote.getDocumentNumber());
        creditNotePayment.setReference(creditNote.getId().toString()); // Guardamos el ID de la NC como referencia.

        repository.save(creditNotePayment);

        // ¡IMPORTANTE! Llamamos a la lógica central para que recalcule el balance.
        recalcularBalancePorPayableId(payableAccount.getId());
    }

    /**
     * Anula el abono (PaymentDetail) que fue generado por una nota de crédito.
     * @param creditNote La nota de crédito que se está anulando.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelPaymentFromCreditNote(PurchaseCreditNote creditNote) {
        // Buscamos el pago específico que se generó a partir de esta nota de crédito usando la referencia.
        PaymentDetails paymentToCancel = repository.findByReferenceAndPaymentMethod(creditNote.getId().toString(), "NOTA_DE_CREDITO")
                .orElseThrow(() -> new NotFoundException("No se encontró el abono generado por la Nota de Crédito ID: " + creditNote.getId()));

        if (!"APLICADO".equals(paymentToCancel.getPaymentStatus())) {
            throw new BusinessRuleException("El abono de la nota de crédito que intenta anular no está en estado APLICADO.");
        }

        paymentToCancel.setPaymentStatus("ANULADO");
        repository.save(paymentToCancel);

        // ¡IMPORTANTE! Volvemos a llamar a la lógica central para que recalcule el balance.
        // Ahora, ignorará este abono anulado y el saldo aumentará correctamente.
        recalcularBalancePorPayableId(paymentToCancel.getAccountsPayable().getId());
    }

}

