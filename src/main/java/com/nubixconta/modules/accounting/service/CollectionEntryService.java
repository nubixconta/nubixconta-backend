package com.nubixconta.modules.accounting.service;
import com.nubixconta.modules.accounting.dto.Account.AccountBankResponseDTO;
import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.entity.CollectionEntry;
import com.nubixconta.modules.accounting.repository.AccountRepository;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.repository.CollectionDetailRepository;
import com.nubixconta.modules.accounting.repository.CollectionEntryRepository;
import com.nubixconta.modules.accountsreceivable.service.CollectionDetailService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CollectionEntryService {

    private final CollectionEntryRepository entryRepository;
    private final CollectionDetailRepository detailRepository;
    private final AccountRepository accountRepository;
    private final ModelMapper mapper;
    private final CollectionDetailRepository collectionDetailRepository;
    private final CollectionDetailService collectionDetailService;

    @Autowired
    public CollectionEntryService(CollectionEntryRepository entryRepository,
                                  CollectionDetailRepository detailRepository,
                                  AccountRepository accountRepository,
                                  CollectionDetailRepository collectionDetailRepository,
                                  ModelMapper mapper,
                                  CollectionDetailService collectionDetailService) {
        this.entryRepository = entryRepository;
        this.detailRepository = detailRepository;
        this.accountRepository = accountRepository;
        this.collectionDetailRepository = collectionDetailRepository;
        this.mapper = mapper;
        this.collectionDetailService = collectionDetailService;
    }

    //Filra solo las cuenta de ACTIVO-BANCO
    public List<AccountBankResponseDTO> findBankAccounts() {
        List<Account> accounts = accountRepository.findByAccountType("ACTIVO-BANCO");
        return accounts.stream()
                .map(account -> mapper.map(account,AccountBankResponseDTO.class)) // convertir a DTO
                .toList();
    }

    //Este metodo busca la cuenta de clientes
    public Integer getClientAccountId() {
        return accountRepository.findClientAccountId()
                .orElseThrow(() -> new RuntimeException("Cuenta 'Cliente(s)' no encontrada"));
    }

    //Crea el asiento contable de CollectionDetail (sucede cuando se aplica un cobro)
    @Transactional
    public void ApplyCollectionDetail(Integer detailId) {
        CollectionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("No se encontró CollectionDetail con ID: " + detailId));

        // Cuenta de banco (viene del detalle)
        Account bankAccount = accountRepository.findById(detail.getAccountId())
                .orElseThrow(() -> new RuntimeException("Cuenta bancaria no encontrada"));

        // Cuenta de cliente (ahora usando el servicio)
        Account clientAccount = accountRepository.findById(getClientAccountId())
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

    @Transactional
    public void cancelByDetailId(Integer detailId) {
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
        collectionDetailService.recalcularBalancePorReceivableId(detail.getAccountReceivable().getId());

    }

}
