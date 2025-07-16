package com.nubixconta.modules.accountsreceivable.service;

import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.repository.AccountRepository;
import com.nubixconta.modules.accounting.service.AccountService;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.entity.CollectionEntry;
import com.nubixconta.modules.accountsreceivable.repository.CollectionDetailRepository;
import com.nubixconta.modules.accountsreceivable.repository.CollectionEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CollectionEntryService {

    private final CollectionEntryRepository entryRepository;
    private final CollectionDetailRepository detailRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final CollectionDetailRepository collectionDetailRepository;

    @Autowired
    public CollectionEntryService(CollectionEntryRepository entryRepository,
                                  CollectionDetailRepository detailRepository,
                                  AccountRepository accountRepository,
                                  AccountService accountService,
                                  CollectionDetailRepository collectionDetailRepository) {
        this.entryRepository = entryRepository;
        this.detailRepository = detailRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.collectionDetailRepository = collectionDetailRepository;
    }

    @Transactional
    public void createEntriesFromDetail(Integer detailId) {
        CollectionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("No se encontró CollectionDetail con ID: " + detailId));

        // Cuenta de banco (viene del detalle)
        Account bankAccount = accountRepository.findById(detail.getAccountId())
                .orElseThrow(() -> new RuntimeException("Cuenta bancaria no encontrada"));

        // Cuenta de cliente (ahora usando el servicio)
        Account clientAccount = accountRepository.findById(accountService.getClientAccountId())
                .orElseThrow(() -> new RuntimeException("Cuenta Clientes no encontrada"));

       detail.setPaymentStatus("APLICADO");

        // Registro al DEBE (Banco)
        CollectionEntry entryDebe = new CollectionEntry();
        entryDebe.setCollectionDetail(detail);
        entryDebe.setAccount(bankAccount);
        entryDebe.setDebit(detail.getPaymentAmount());
        entryDebe.setCredit(BigDecimal.ZERO);
        entryDebe.setDescription(detail.getPaymentDetailDescription());
        entryDebe.setDate(LocalDateTime.now());

        // Registro al HABER (Clientes)
        CollectionEntry entryHaber = new CollectionEntry();
        entryHaber.setCollectionDetail(detail);
        entryHaber.setAccount(clientAccount);
        entryHaber.setDebit(BigDecimal.ZERO);
        entryHaber.setCredit(detail.getPaymentAmount());
        entryHaber.setDescription(detail.getPaymentDetailDescription());
        entryHaber.setDate(LocalDateTime.now());

        // Guardar ambos
        collectionDetailRepository.save(detail);
        entryRepository.save(entryDebe);
        entryRepository.save(entryHaber);
    }

    public void deleteById(Integer id) {
        entryRepository.deleteById(id);
    }
    @Transactional
    public void deleteEntriesByDetailId(Integer detailId) {
        // Validación opcional
        if (!detailRepository.existsById(detailId)) {
            throw new RuntimeException("No existe un detalle con ID: " + detailId);
        }

        entryRepository.deleteByCollectionDetailId(detailId);

        // Cambiar el estado del CollectionDetail a "ANULADO" si quieres
        CollectionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));

        detail.setPaymentStatus("ANULADO");
        detailRepository.save(detail);
    }

}
