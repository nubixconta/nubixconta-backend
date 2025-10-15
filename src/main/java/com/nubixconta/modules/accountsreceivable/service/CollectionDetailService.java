package com.nubixconta.modules.accountsreceivable.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailCreateDTO;
import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailUpdateDTO;
import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.repository.AccountsReceivableRepository;
import com.nubixconta.modules.accountsreceivable.repository.CollectionDetailRepository;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.repository.SaleRepository;
import com.nubixconta.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CollectionDetailService {

    private final CollectionDetailRepository repository;
    private final SaleRepository saleRepository;
    private ChangeHistoryService changeHistoryService;
    private final CompanyRepository companyRepository;

    @Autowired
    private AccountsReceivableRepository accountsReceivableRepository;
    public CollectionDetailService(CollectionDetailRepository repository,
                                   SaleRepository saleRepository,
                                   ChangeHistoryService changeHistoryService,
                                   CompanyRepository companyRepository) {
        this.repository = repository;
        this.saleRepository = saleRepository;
        this.changeHistoryService = changeHistoryService;
        this.companyRepository = companyRepository;
    }

    // Helper privado para obtener el contexto de la empresa de forma segura y consistente.
    public Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

    //Metoo para traer todos los cobros filtrados por empresa
    public List<CollectionDetail> findAll() {
        Integer companyId = getCompanyIdFromContext();
        return repository.findByCompanyId(companyId);
    }

    public Optional<CollectionDetail> findById(Integer id) {
        return repository.findById(id);
    }
    //Metodo para filtrar por fechas
    public List<CollectionDetail> findByDateRange(LocalDateTime start, LocalDateTime end) {
        Integer companyId = getCompanyIdFromContext();
        // Llamar al nuevo método del repositorio.
        return repository.findByCompanyIdAndCollectionDetailDateBetween(companyId, start, end);
    }

    public CollectionDetail save(CollectionDetail detail) {
        if (detail.getAccountReceivable() == null || detail.getAccountReceivable().getId() == null) {
            throw new IllegalArgumentException("Debe incluir el objeto accountReceivable con su idPurchaseCreditNote");
        }

        // Buscar la entidad completa y setearla
        Integer id = detail.getAccountReceivable().getId();
        var accountReceivable = accountsReceivableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe accountReceivable con ID: " + id));

        detail.setAccountReceivable(accountReceivable);
        return repository.save(detail);
    }


    @Transactional
    public void deleteById(Integer id) {
        CollectionDetail detail = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el detalle con ID: " + id));

        //  Solo se puede eliminar si NO ha sido aplicado
        if ("APLICADO".equalsIgnoreCase(detail.getPaymentStatus())) {
            throw new IllegalStateException("No se puede eliminar un cobro ya aplicado.");
        }

        Integer receivableId = detail.getAccountReceivable().getId();

        // Primero eliminar
        repository.deleteById(id);

        // Luego recalcular el saldo
        recalcularBalancePorReceivableId(receivableId);
    }


    //Metodo para registrar un acountsReceivable automaticamante creando primero acountsReceivable y actualizando su
    //saldo si es un cobro parcial

    @Transactional
    public CollectionDetail registerPayment(CollectionDetailCreateDTO dto) {
        Integer saleId = dto.getSaleId();
        Integer companyId = getCompanyIdFromContext();

        Sale sale = saleRepository.findBySaleIdAndCompanyId(saleId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada o no pertenece a la empresa actual."));


        // **Llamada al nuevo método reutilizable**
        AccountsReceivable ar = accountsReceivableRepository.findBySale(sale)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta por cobrar no encontrada para la venta. Asegúrese de que la venta haya sido aplicada."));


        BigDecimal montoTotalVenta = ar.getSale().getTotalAmount();
        BigDecimal saldoActual = ar.getBalance();
        BigDecimal abonoNuevo = dto.getPaymentAmount();
        BigDecimal nuevoSaldo = saldoActual.subtract(abonoNuevo);

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto a abonar excede el saldo restante de la venta.");
        }
        // Actualizar el balance
        ar.setBalance(nuevoSaldo);
        accountsReceivableRepository.save(ar);

        //Obtener la referencia a la empresa.
        Company companyRef = companyRepository.getReferenceById(companyId);

        // Crear CollectionDetail desde DTO
        CollectionDetail detail = new CollectionDetail();
        detail.setAccountReceivable(ar);
        detail.setCompany(companyRef);
        detail.setAccountId(dto.getAccountId());
        detail.setReference(dto.getReference());
        detail.setPaymentMethod(dto.getPaymentMethod());
        detail.setPaymentStatus(dto.getPaymentStatus());
        detail.setPaymentAmount(dto.getPaymentAmount());
        detail.setPaymentDetailDescription(dto.getPaymentDetailDescription());
        detail.setCollectionDetailDate(dto.getCollectionDetailDate());
        detail.setModuleType(dto.getModuleType());

        CollectionDetail saved = repository.save(detail);
        recalcularBalancePorReceivableId(ar.getId());

        // Bitácora de cambios
        changeHistoryService.logChange(
                "Cuentas por cobrar",
                "Se realizo un cobro para el numero de documento " + ar.getSale().getDocumentNumber()
                + "A la empresa " + companyRef.getCompanyName()
        );


        return saved;
    }

    /**
     * Método reutilizable para buscar una cuenta por cobrar asociada a una venta o crearla si no existe.
     * Este método asume que la validación de la venta (si existe y esta aplicada y pertenece a la empresa)
     * @param sale La entidad Venta para la cual se busca o crea la cuenta por cobrar.
     * @return La entidad AccountsReceivable existente o recién creada.
     */
    public AccountsReceivable findOrCreateAccountsReceivable(Sale sale) {
        // Se busca por el ID de la venta y el ID de la compañía para asegurar la pertenencia de los datos.
        return accountsReceivableRepository.findBySaleIdAndCompanyId(sale.getSaleId(), sale.getCompany().getId())
                .orElseGet(() -> {
                    // Si no existe, se crea una nueva instancia.
                    AccountsReceivable newAR = new AccountsReceivable();
                    newAR.setSaleId(sale.getSaleId());
                    newAR.setSale(sale);
                    newAR.setBalance(sale.getTotalAmount()); // El balance inicial es igual al monto total de la venta
                    newAR.setModuleType("Cuentas por cobrar");
                    newAR.setCompany(sale.getCompany());
                    return accountsReceivableRepository.save(newAR);
                });
    }
        @Transactional
    public void recalcularBalancePorReceivableId(Integer receivableId) {
        var ar = accountsReceivableRepository.findById(receivableId)
                .orElseThrow(() -> new RuntimeException("AccountsReceivable no encontrado"));

        // Obtener solo los abonos que NO estén anulados
        Integer companyId = getCompanyIdFromContext();
        List<CollectionDetail> abonos = repository.findByAccountReceivableIdAndCompanyId(receivableId,companyId).stream()
                .filter(detalle -> !"ANULADO".equalsIgnoreCase(detalle.getPaymentStatus()))
                .toList();

        BigDecimal totalAbonos = abonos.stream()
                .map(CollectionDetail::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Sale venta = ar.getSale();
        if (venta == null) {
            throw new IllegalStateException("La relación con la venta no está disponible para esta cuenta por cobrar.");
        }

        BigDecimal montoTotalVenta = venta.getTotalAmount();
        BigDecimal nuevoBalance = montoTotalVenta.subtract(totalAbonos);

        if (nuevoBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("El balance calculado es negativo, lo que indica un error en los datos.");
        }
        ar.setBalance(nuevoBalance);
        accountsReceivableRepository.save(ar);
    }
}
