package com.nubixconta.modules.accountsreceivable.service;

import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.repository.AccountsReceivableRepository;
import com.nubixconta.modules.accountsreceivable.repository.CollectionDetailRepository;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.repository.SaleRepository;
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

    @Autowired
    private AccountsReceivableRepository accountsReceivableRepository;
    public CollectionDetailService(CollectionDetailRepository repository,SaleRepository saleRepository) {
        this.repository = repository;
        this.saleRepository = saleRepository;
    }

    public List<CollectionDetail> findAll() {
        return repository.findAll();
    }

    public Optional<CollectionDetail> findById(Integer id) {
        return repository.findById(id);
    }
    //Metodo para filtrar por fechas
    public List<CollectionDetail> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByDateRange(start, end);
    }

    public CollectionDetail save(CollectionDetail detail) {
        if (detail.getAccountReceivable() == null || detail.getAccountReceivable().getId() == null) {
            throw new IllegalArgumentException("Debe incluir el objeto accountReceivable con su id");
        }

        // Buscar la entidad completa y setearla
        Integer id = detail.getAccountReceivable().getId();
        var accountReceivable = accountsReceivableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe accountReceivable con ID: " + id));

        detail.setAccountReceivable(accountReceivable);
        return repository.save(detail);
    }


    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    public CollectionDetail update(Integer id, CollectionDetail updated) {


        if (updated.getAccountReceivable() == null || updated.getAccountReceivable().getId() == null) {
            throw new IllegalArgumentException("Debe incluir el objeto accountReceivable con su id");
        }

        var accountReceivableId = updated.getAccountReceivable().getId();
        var accountReceivable = accountsReceivableRepository.findById(accountReceivableId)
                .orElseThrow(() -> new RuntimeException("No existe accountReceivable con ID: " + accountReceivableId));

        var existing = repository.findById(id);


        return existing
                .map(current -> {
                    current.setAccountReceivable(accountReceivable);
                    current.setAccountId(updated.getAccountId());
                    current.setReference(updated.getReference());
                    current.setPaymentMethod(updated.getPaymentMethod());
                    current.setPaymentStatus(updated.getPaymentStatus());
                    current.setPaymentAmount(updated.getPaymentAmount());
                    current.setPaymentDetailDescription(updated.getPaymentDetailDescription());
                    current.setModuleType(updated.getModuleType());

                    return repository.save(current);
                })
                .orElseThrow(() -> new RuntimeException(" Detalle no encontrado con ID: " + id));
    }

    //Metodo para registrar un acountsReceivable automaticamante creando primero acountsReceivable y actualizando su
    //saldo si es un cobro parcial

    @Transactional
    public CollectionDetail registerPayment(CollectionDetail detail) {
        var saleId = detail.getAccountReceivable().getSaleId();

        //Buscar si ya existe un registro de accountRecivable por saleId
        var ar = accountsReceivableRepository.findBySaleId(saleId)
                .orElseGet(() -> {
                    Sale sale = saleRepository.findById(saleId)
                            .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada"));
                    //si no existe, se crea uno nuevo cuyo saldo = monto total de la venta
                    AccountsReceivable nuevo = new AccountsReceivable();
                    nuevo.setSaleId(saleId);
                    nuevo.setSale(sale);
                    nuevo.setBalance(BigDecimal.ZERO);
                    nuevo.setModuleType("Cuentas por cobrar");
                    return accountsReceivableRepository.save(nuevo);
                });
        if (ar.getSale() == null) {
            ar = accountsReceivableRepository.findById(ar.getId()).orElseThrow();
        }

        var montoTotalVenta = ar.getSale().getTotalAmount();
        var saldoActual = ar.getBalance();
        var abonoNuevo = detail.getPaymentAmount();

        if (saldoActual.add(abonoNuevo).compareTo(montoTotalVenta) > 0) {
            throw new IllegalArgumentException("El monto a abonar excede el monto total de la venta.");
        }

        //Actualizar el balance sumando el abono
        ar.setBalance(saldoActual.add(abonoNuevo));
        accountsReceivableRepository.save(ar);

        detail.setAccountReceivable(ar);
        CollectionDetail savedDetail = repository.save(detail);
        recalcularBalancePorReceivableId(ar.getId());
        return savedDetail;

    }
    @Transactional
    public void recalcularBalancePorReceivableId(Integer receivableId) {
        var ar = accountsReceivableRepository.findById(receivableId)
                .orElseThrow(() -> new RuntimeException("AccountsReceivable no encontrado"));

        List<CollectionDetail> abonos = repository.findByAccountReceivableId(receivableId);

        BigDecimal totalAbonos = abonos.stream()
                .map(CollectionDetail::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Sale venta = ar.getSale();
        if (venta == null) {
            throw new IllegalStateException("La relación con la venta no está disponible para esta cuenta por cobrar.");
        }

        BigDecimal montoTotalVenta = venta.getTotalAmount();
        if (totalAbonos.compareTo(montoTotalVenta) > 0) {
            throw new IllegalArgumentException("La suma total de abonos excede el monto total de la venta.");
        }


        ar.setBalance(totalAbonos);
        accountsReceivableRepository.save(ar);
    }

}
