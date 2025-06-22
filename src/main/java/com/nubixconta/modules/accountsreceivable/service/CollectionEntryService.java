package com.nubixconta.modules.accountsreceivable.service;

import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.repository.AccountRepository;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.entity.CollectionEntry;
import com.nubixconta.modules.accountsreceivable.repository.CollectionDetailRepository;
import com.nubixconta.modules.accountsreceivable.repository.CollectionEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CollectionEntryService {

    private final CollectionEntryRepository entryRepository;
    private final CollectionDetailRepository detailRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public CollectionEntryService(CollectionEntryRepository entryRepository,
                                  CollectionDetailRepository detailRepository,
                                  AccountRepository accountRepository) {
        this.entryRepository = entryRepository;
        this.detailRepository = detailRepository;
        this.accountRepository = accountRepository;
    }

    public void createEntriesFromDetail(Integer detailId) {
        CollectionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("No se encontró CollectionDetail con ID: " + detailId));

        Account originalAccount = accountRepository.findById(detail.getAccountId())
                .orElseThrow(() -> new RuntimeException("No se encontró cuenta con ID: " + detail.getAccountId()));

        Account clientAccount = accountRepository
                .findByAccountNameIgnoreCase("Clientes")
                .orElseThrow(() -> new RuntimeException("No se encontró cuenta con nombre exacto 'Clientes'"));

        // Registro original (debe)
        CollectionEntry original = new CollectionEntry();
        original.setCollectionDetail(detail);
        original.setAccount(originalAccount);
        original.setDebit(detail.getPaymentAmount());
        original.setCredit(BigDecimal.ZERO);
        original.setDescription(detail.getPaymentDetailDescription());
        original.setDate(LocalDateTime.now());

        // Contra cuenta (haber)
        CollectionEntry contraEntry = new CollectionEntry();
        contraEntry.setCollectionDetail(detail);
        contraEntry.setAccount(clientAccount);
        contraEntry.setDebit(BigDecimal.ZERO);
        contraEntry.setCredit(detail.getPaymentAmount());
        contraEntry.setDescription(detail.getPaymentDetailDescription());
        contraEntry.setDate(LocalDateTime.now());

        // Guardar ambos registros
        entryRepository.save(original);
        entryRepository.save(contraEntry);
    }

    public void deleteById(Integer id) {
        entryRepository.deleteById(id);
    }
}
