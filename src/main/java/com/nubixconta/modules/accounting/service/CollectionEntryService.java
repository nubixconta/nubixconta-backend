package com.nubixconta.modules.accounting.service;
import com.nubixconta.modules.AccountsPayable.repository.PaymentDetailsRepository;
import com.nubixconta.modules.accounting.dto.Account.AccountBankResponseDTO;
import com.nubixconta.modules.accounting.dto.CollectionEntry.CollectionEntryFronBankResponseDTO;
import com.nubixconta.modules.accounting.dto.CollectionEntry.CollectionEntryResponseDTO;
import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.entity.CollectionEntry;
import com.nubixconta.modules.accounting.entity.PaymentEntry;
import com.nubixconta.modules.accounting.repository.AccountRepository;
import com.nubixconta.modules.accounting.repository.CatalogRepository;
import com.nubixconta.modules.accounting.repository.PaymentEntryRepository;
import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailFromEntryResponseDTO;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.repository.CollectionDetailRepository;
import com.nubixconta.modules.accounting.repository.CollectionEntryRepository;
import com.nubixconta.modules.accountsreceivable.service.CollectionDetailService;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.repository.SaleRepository;
import com.nubixconta.security.JwtUtil;
import org.springframework.transaction.annotation.Transactional;
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
public class CollectionEntryService {

    private final CollectionEntryRepository entryRepository;
    private final AccountRepository accountRepository;
    private final ModelMapper mapper;
    private final SaleRepository saleRepository;
    private final CatalogRepository catalogRepository;
    private final CollectionDetailRepository collectionDetailRepository;
    private final CollectionDetailService collectionDetailService;
    private final PaymentEntryRepository paymentEntryRepository;
    private final PaymentDetailsRepository paymentDetailsRepository;
    @Autowired
    public CollectionEntryService(CollectionEntryRepository entryRepository,
                                  AccountRepository accountRepository,
                                  CollectionDetailRepository collectionDetailRepository,
                                  ModelMapper mapper,
                                  CatalogRepository catalogRepository,
                                  CollectionDetailService collectionDetailService,
                                  SaleRepository saleRepository,
                                  PaymentEntryRepository paymentEntryRepository,
                                  PaymentDetailsRepository paymentDetailsRepository) {
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
        this.catalogRepository = catalogRepository;
        this.collectionDetailRepository = collectionDetailRepository;
        this.saleRepository = saleRepository;
        this.mapper = mapper;
        this.collectionDetailService = collectionDetailService;
        this.paymentEntryRepository = paymentEntryRepository;
        this.paymentDetailsRepository = paymentDetailsRepository;
    }

    // Este metodo mapea en el dto el numero de documento, el nombre del cliente de Sale
    //Tambien mapea en el dto el status y tipo de collectionDetail
    //mapea en el dto la informacion del asiento contable
    //El metodo se ocupa para visualizar el asiento contable en CXC
    public List<CollectionEntryResponseDTO> getEntriesByDetailId(Integer detailId) {
        List<CollectionEntry> entries = entryRepository.findByCollectionDetail_Id(detailId);

        // Si no hay entradas, puedes devolver una lista vacía
        if (entries.isEmpty()) {
            return List.of();
        }

        // Obtener el CollectionDetail y la Venta (Sale) una sola vez para evitar consultas N+1
        CollectionDetail detail = entries.get(0).getCollectionDetail();
        Sale sale = saleRepository.findById(detail.getAccountReceivable().getSaleId())
                .orElseThrow(() -> new RuntimeException("Venta no encontrada para el detalle de cobro."));

        return entries.stream()
                .map(entry -> {
                    CollectionEntryResponseDTO dto = mapper.map(entry, CollectionEntryResponseDTO.class);

                    // Mapear campos de CollectionEntry
                    dto.setCodAccount(entry.getCatalog().getAccount().getGeneratedCode());
                    dto.setAccountName(entry.getCatalog().getAccount().getAccountName());

                    // Mapear campos de CollectionDetail
                    dto.setTipo(detail.getModuleType());
                    dto.setStatus(detail.getPaymentStatus());

                    // Mapear campos de AccountsReceivable y Sale
                    dto.setDocumentNumber(sale.getDocumentNumber());
                    dto.setCustumerName(sale.getCustomer().getCustomerName() + " " + sale.getCustomer().getCustomerLastName());

                    return dto;
                })
                .collect(Collectors.toList());
    }




    // Método para obtener el ID de la cuenta de Clientes desde la tabla de cuenta
    public Account getClientAccount() {
        return accountRepository.findClientAccount()
                .orElseThrow(() -> new RuntimeException("Cuenta 'Clientes' no encontrada"));
    }
    // Nuevo método para encontrar un Catalog basado en un Account y un CompanyId
    private Catalog findCatalog( Integer companyId,Integer accountId) {
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
    public void ApplyCollectionDetail(Integer detailId) {

        // Llama al nuevo método para obtener el ID de la empresa
        Integer companyId = getCompanyIdFromToken();

        CollectionDetail detail = collectionDetailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("No se encontró CollectionDetail con ID: " + detailId));
        // Obtener el Catalog para la cuenta bancaria usando el companyId y el accountId del detalle
        // NOTA: detail.getAccountId() es el ID de la cuenta bancaria maestra asociada al detalle
        Catalog bankCatalog = findCatalog( companyId,detail.getAccountId());


        // Cuenta de cliente (ahora usando el servicio)
        Integer clientAccountId = getClientAccount().getId();
        Catalog clientCatalog = findCatalog( companyId,clientAccountId);

        detail.setPaymentStatus("APLICADO");

        // Registro al DEBE (Banco)
        CollectionEntry entryDebe = new CollectionEntry();
        entryDebe.setCollectionDetail(detail);
        entryDebe.setCatalog(bankCatalog);
        entryDebe.setDebit(detail.getPaymentAmount());
        entryDebe.setCredit(BigDecimal.ZERO);
        entryDebe.setDescription(detail.getPaymentDetailDescription());
        entryDebe.setDate(LocalDateTime.now());

        // Registro al HABER (Cliente)
        CollectionEntry entryHaber = new CollectionEntry();
        entryHaber.setCollectionDetail(detail);
        entryHaber.setCatalog(clientCatalog); // Usar el objeto Catalog
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
        if (!collectionDetailRepository.existsById(detailId)) {
            throw new RuntimeException("No existe un detalle con ID: " + detailId);
        }


        entryRepository.deleteByCollectionDetailId(detailId);

        // Cambiar el estado del CollectionDetail a "ANULADO" si quieres
        CollectionDetail detail = collectionDetailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));

        detail.setPaymentStatus("ANULADO");
        collectionDetailRepository.save(detail);
        collectionDetailService.recalcularBalancePorReceivableId(detail.getAccountReceivable().getId());

    }
    // *** Método existente para CollectionEntry, modificado para ser más genérico si quieres o dejarlo ***
    @Transactional // Mantener @Transactional aquí
    public List<CollectionEntryFronBankResponseDTO> getCollectionEntriesForBank(Integer collectionDetailId) {
        List<CollectionEntry> entries = entryRepository.findByCollectionDetail_Id(collectionDetailId);

        // Si no hay CollectionDetail, o la lista está vacía, no se crea el detailDto
        if (entries.isEmpty()) {
            return List.of();
        }

        CollectionDetail collectionDetail = entries.get(0).getCollectionDetail();
        CollectionDetailFromEntryResponseDTO detailDto = mapper.map(collectionDetail, CollectionDetailFromEntryResponseDTO.class);
        detailDto.setModuleType(collectionDetail.getModuleType());
        detailDto.setReference(String.valueOf(collectionDetail.getId()));

        return entries.stream()
                .filter(entry ->
                        entry.getCatalog() != null &&
                                entry.getCatalog().getAccount() != null &&
                                "ACTIVO-BANCO".equals(entry.getCatalog().getAccount().getAccountType())
                )
                .map(entry -> {
                    CollectionEntryFronBankResponseDTO dto = new CollectionEntryFronBankResponseDTO();
                    dto.setId(entry.getId());
                    dto.setDebit(entry.getDebit());
                    dto.setCredit(entry.getCredit());
                    dto.setDescription(entry.getDescription());
                    dto.setDate(entry.getDate());

                    Catalog catalog = entry.getCatalog();
                    if (catalog != null && catalog.getAccount() != null) {

                        dto.setAccountName(catalog.getAccount().getAccountName());
                    }

                    dto.setCollection(detailDto); // En este contexto, 'collection' se refiere al detalle de CollectionEntry
                    return dto;
                }).collect(Collectors.toList());
    }
    // *** NUEVOS MÉTODOS PARA OBTENER TODAS LAS ENTRADAS DE UN TIPO CON EL FILTRO ***

    @Transactional(readOnly = true) // readOnly = true es una buena práctica para métodos de solo lectura
    public List<CollectionEntryFronBankResponseDTO> getAllCollectionEntriesForBank() {
        List<CollectionEntry> entries = entryRepository.findAllWithCatalogAndAccount(); // Usamos el nuevo método del repositorio

        return entries.stream()
                .filter(entry ->
                        entry.getCatalog() != null &&
                                entry.getCatalog().getAccount() != null &&
                                "ACTIVO-BANCO".equals(entry.getCatalog().getAccount().getAccountType())
                )
                .map(entry -> {
                    CollectionEntryFronBankResponseDTO dto = new CollectionEntryFronBankResponseDTO();
                    dto.setId(entry.getId());
                    dto.setDebit(entry.getDebit());
                    dto.setCredit(entry.getCredit());
                    dto.setDescription(entry.getDescription());
                    dto.setDate(entry.getDate());

                    Catalog catalog = entry.getCatalog();
                    dto.setAccountName(catalog.getAccount().getAccountName());

                    // Mapear el CollectionDetail asociado
                    CollectionDetailFromEntryResponseDTO detailDto = mapper.map(entry.getCollectionDetail(), CollectionDetailFromEntryResponseDTO.class);
                    detailDto.setModuleType(entry.getCollectionDetail().getModuleType());
                    detailDto.setReference(String.valueOf(entry.getCollectionDetail().getReference()));
                    dto.setCollection(detailDto);

                    return dto;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CollectionEntryFronBankResponseDTO> getAllPaymentEntriesForBank() {
        List<PaymentEntry> entries = paymentEntryRepository.findAllWithCatalogAndAccount(); // Usamos el nuevo método del repositorio

        return entries.stream()
                .filter(entry ->
                        entry.getCatalog() != null &&
                                entry.getCatalog().getAccount() != null &&
                                "ACTIVO-BANCO".equals(entry.getCatalog().getAccount().getAccountType())
                )
                .map(entry -> {
                    CollectionEntryFronBankResponseDTO dto = new CollectionEntryFronBankResponseDTO();
                    dto.setId(entry.getId());
                    dto.setDebit(entry.getDebit());
                    dto.setCredit(entry.getCredit());
                    dto.setDescription(entry.getDescription());
                    dto.setDate(entry.getDate());

                    Catalog catalog = entry.getCatalog();
                    dto.setAccountName(catalog.getAccount().getAccountName());

                    // Mapear el PaymentDetails asociado
                    // Reutilizamos CollectionDetailFromEntryResponseDTO para PaymentDetails
                    CollectionDetailFromEntryResponseDTO detailDto = new CollectionDetailFromEntryResponseDTO();
                    detailDto.setId(entry.getPaymentDetails().getId());
                    detailDto.setModuleType(entry.getPaymentDetails().getModuleType());
                    detailDto.setReference(entry.getPaymentDetails().getReference());
                    dto.setCollection(detailDto);

                    return dto;
                }).collect(Collectors.toList());
    }

}
