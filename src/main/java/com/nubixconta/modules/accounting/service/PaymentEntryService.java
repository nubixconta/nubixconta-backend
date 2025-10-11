package com.nubixconta.modules.accounting.service;

import com.nubixconta.modules.AccountsPayable.entity.PaymentDetails;
import com.nubixconta.modules.AccountsPayable.repository.PaymentDetailsRepository;
import com.nubixconta.modules.AccountsPayable.service.PaymentDetailsService;
import com.nubixconta.modules.accounting.dto.Account.AccountBankResponseDTO;
import com.nubixconta.modules.accounting.dto.PaymentEntry.PaymentEntryResponseDTO;
import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.entity.PaymentEntry;
import com.nubixconta.modules.accounting.repository.AccountRepository;
import com.nubixconta.modules.accounting.repository.CatalogRepository;
import com.nubixconta.modules.accounting.repository.PaymentEntryRepository;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.modules.purchases.repository.PurchaseRepository;
import com.nubixconta.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class PaymentEntryService {
    private final PaymentEntryRepository entryRepository;
    private final AccountRepository accountRepository;
    private final ModelMapper mapper;
    private final PurchaseRepository purchaseRepository;
    private final CatalogRepository catalogRepository;
    private final PaymentDetailsRepository paymentDetailsRepository;
    private final PaymentDetailsService paymentDetailsService;

    @Autowired
    public PaymentEntryService(PaymentEntryRepository entryRepository,
                                  AccountRepository accountRepository,
                                  PaymentDetailsRepository paymentDetailsRepository,
                                  ModelMapper mapper,
                                  CatalogRepository catalogRepository,
                                  PaymentDetailsService paymentDetailsService,
                                  PurchaseRepository purchaseRepository) {
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
        this.catalogRepository = catalogRepository;
        this.paymentDetailsRepository= paymentDetailsRepository;
        this.purchaseRepository = purchaseRepository;
        this.mapper = mapper;
        this.paymentDetailsService = paymentDetailsService;
    }

    // Este metodo mapea en el dto el numero de documento, el nombre del cliente de Sale
    //Tambien mapea en el dto el status y tipo de collectionDetail
    //mapea en el dto la informacion del asiento contable
    //El metodo se ocupa para visualizar el asiento contable en CXC

    public List<PaymentEntryResponseDTO> getEntriesByDetailId(Integer detailId) {
        List<PaymentEntry> entries = entryRepository.findByPaymentDetails_Id(detailId);

        // Si no hay entradas, puedes devolver una lista vacía
        if (entries.isEmpty()) {
            return List.of();
        }

        // Obtener el CollectionDetail y la Venta (Sale) una sola vez para evitar consultas N+1
        PaymentDetails detail = entries.get(0).getPaymentDetails();
        Purchase purchase = purchaseRepository.findById(detail.getAccountsPayable().getPurchaseId())
                .orElseThrow(() -> new RuntimeException("Compra no encontrada para el detalle de pago."));

        return entries.stream()
                .map(entry -> {
                    PaymentEntryResponseDTO dto = mapper.map(entry, PaymentEntryResponseDTO.class);

                    // Mapear campos de CollectionEntry
                    dto.setCodAccount(entry.getCatalog().getAccount().getGeneratedCode());
                    dto.setAccountName(entry.getCatalog().getAccount().getAccountName());

                    // Mapear campos de PaymentDetail
                    dto.setTipo(detail.getModuleType());
                    dto.setStatus(detail.getPaymentStatus());

                    // Mapear campos de AccountsPayable y Purchase
                    dto.setDocumentNumber(purchase.getDocumentNumber());
                    dto.setSupplierName(purchase.getSupplier().getSupplierName() + " " + purchase.getSupplier().getSupplierLastName());

                    return dto;
                })
                .collect(Collectors.toList());
    }




    // Método para obtener el ID de la cuenta de Clientes desde la tabla de cuenta
    public Account getSupplierAccount() {
        return accountRepository.findSupplierAccount()
                .orElseThrow(() -> new RuntimeException("Cuenta 'Proveedores' no encontrada"));
    }
    // Nuevo método para encontrar un Catalog basado en un Account y un CompanyId
    private Catalog findCatalog(Integer companyId, Integer accountId) {
        return catalogRepository.findByCompany_IdAndAccount_Id(companyId, accountId)
                .orElseThrow(() -> new RuntimeException("Catalog no encontrado para la cuenta maestra ID " + accountId + " y empresa ID " + companyId));
    }

    //Filra solo las cuenta de ACTIVO-BANCO
    public List<AccountBankResponseDTO> findBankAccounts() {
        List<Account> accounts = accountRepository.findByAccountType("ACTIVO-BANCO");
        return accounts.stream()
                .map(account -> mapper.map(account,AccountBankResponseDTO.class)) // convertir a DTO
                .toList();
    }
    //  Obtener el ID de la empresa del token
    private Integer getCompanyIdFromToken() {
        String bearerToken = Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader("Authorization"))
                .orElseThrow(() -> new RuntimeException("No se pudo obtener el token de autorización de la solicitud."));

        if (!bearerToken.startsWith("Bearer ")) {
            throw new RuntimeException("Formato de token JWT inválido. No empieza con 'Bearer '.");
        }
        String jwtToken = bearerToken.replace("Bearer ", "");

        return JwtUtil.extractCompanyId(jwtToken)
                .orElseThrow(() -> new RuntimeException("No se pudo obtener el ID de la empresa del token JWT. Asegúrate que el token contiene el 'company_id' claim."));
    }

    //Crea el asiento contable de CollectionDetail (sucede cuando se aplica un cobro)
    @Transactional
    public void ApplyPaymentDetail(Integer detailId) {

        // Llama al nuevo método para obtener el ID de la empresa
        Integer companyId = getCompanyIdFromToken();

        PaymentDetails detail = paymentDetailsRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("No se encontró CollectionDetail con ID: " + detailId));
        // Obtener el Catalog para la cuenta bancaria usando el companyId y el accountId del detalle
        // NOTA: detail.getAccountId() es el ID de la cuenta bancaria maestra asociada al detalle
        Catalog bankCatalog = findCatalog( companyId,detail.getAccountId());


        // Cuenta de cliente (ahora usando el servicio)
        Integer suppplierAccountId = getSupplierAccount().getId();
        Catalog clientCatalog = findCatalog( companyId,suppplierAccountId);

        detail.setPaymentStatus("APLICADO");

        // Registro al DEBE (Banco)
        PaymentEntry entryDebe = new PaymentEntry();
        entryDebe.setPaymentDetails(detail);
        entryDebe.setCatalog(bankCatalog);
        entryDebe.setDebit(detail.getPaymentAmount());
        entryDebe.setCredit(BigDecimal.ZERO);
        entryDebe.setDescription(detail.getPaymentDetailDescription());
        entryDebe.setDate(LocalDateTime.now());

        // Registro al HABER (Proveedor)
        PaymentEntry entryHaber = new PaymentEntry();
        entryHaber.setPaymentDetails(detail);
        entryHaber.setCatalog(clientCatalog); // Usar el objeto Catalog
        entryHaber.setDebit(BigDecimal.ZERO);
        entryHaber.setCredit(detail.getPaymentAmount());
        entryHaber.setDescription(detail.getPaymentDetailDescription());
        entryHaber.setDate(LocalDateTime.now());

        // Guardar ambos
        paymentDetailsRepository.save(detail);
        entryRepository.save(entryDebe);
        entryRepository.save(entryHaber);
    }

    @Transactional
    public void cancelByDetailId(Integer detailId) {
        // Validación opcional
        if (!paymentDetailsRepository.existsById(detailId)) {
            throw new RuntimeException("No existe un detalle con ID: " + detailId);
        }


        entryRepository.deleteByPaymentDetailsId(detailId);

        // Cambiar el estado del CollectionDetail a "ANULADO" si quieres
        PaymentDetails detail = paymentDetailsRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));

        detail.setPaymentStatus("ANULADO");
        paymentDetailsRepository.save(detail);
        paymentDetailsService.recalcularBalancePorPayableId(detail.getAccountsPayable().getId());

    }
}
