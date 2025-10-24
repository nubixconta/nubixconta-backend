package com.nubixconta.modules.purchases.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.AccountsPayable.service.AccountsPayableService;
import com.nubixconta.modules.AccountsPayable.service.PaymentDetailsService;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.service.CatalogService;
import com.nubixconta.modules.accounting.service.PurchasesAccountingService;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.service.InventoryService;
import com.nubixconta.modules.inventory.service.ProductService;
import com.nubixconta.modules.purchases.dto.creditnote.*;
import com.nubixconta.modules.purchases.entity.*;
import com.nubixconta.modules.purchases.repository.PurchaseCreditNoteRepository;
import com.nubixconta.modules.purchases.repository.PurchaseRepository;
import com.nubixconta.modules.purchases.repository.SupplierRepository;
import com.nubixconta.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseCreditNoteService {

    // --- Repositorios y Servicios Principales ---
    private final PurchaseCreditNoteRepository creditNoteRepository;
    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;
    private final ProductService productService;
    private final CatalogService catalogService;
    private final ModelMapper modelMapper;

    // --- Servicios para Orquestación Transaccional ---
    private final InventoryService inventoryService;
    private final PurchasesAccountingService purchasesAccountingService;
    private final AccountsPayableService accountsPayableService;
    private final ChangeHistoryService changeHistoryService;
    private final PaymentDetailsService paymentDetailsService;


    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

    // =========================================================================================
    // == MÉTODOS PÚBLICOS DEL CICLO DE VIDA
    // =========================================================================================

    @Transactional
    public PurchaseCreditNoteResponseDTO createCreditNote(PurchaseCreditNoteCreateDTO dto) {
        Integer companyId = getCompanyIdFromContext();

        // 1. Validaciones de negocio iniciales
        if (creditNoteRepository.existsByCompany_IdAndDocumentNumber(companyId, dto.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe una nota de crédito con el número de documento: " + dto.getDocumentNumber());
        }

        Purchase purchase = purchaseRepository.findById(dto.getPurchaseId())
                .orElseThrow(() -> new NotFoundException("La compra con ID " + dto.getPurchaseId() + " no fue encontrada."));

        if (!purchase.getCompany().getId().equals(companyId)) {
            throw new BusinessRuleException("Acción no permitida: La compra asociada no pertenece a la empresa seleccionada.");
        }

        if (!"APLICADA".equals(purchase.getPurchaseStatus())) {
            throw new BusinessRuleException("Solo se pueden crear notas de crédito sobre compras en estado APLICADA.");
        }

        // --- ¡VALIDACIÓN DE PAGOS ACTIVADA! ---
        // Se comprueba si la compra tiene pagos activos (PENDIENTES o APLICADOS).
        // Si es así, se bloquea la creación de la nota de crédito.
        boolean hasActivePayments = accountsPayableService.validatePurchaseWithoutCollections(purchase.getIdPurchase());
        if (hasActivePayments) {
            throw new BusinessRuleException(
                    "No se puede crear la nota de crédito porque la compra asociada ya tiene pagos registrados. " +
                            "Por favor, anule primero los pagos en el módulo de Cuentas por Pagar."
            );
        }
        // --- FIN DE LA VALIDACIÓN ---

        List<String> activeStatuses = List.of("PENDIENTE", "APLICADA");
        if (creditNoteRepository.existsByCompany_IdAndPurchase_IdPurchaseAndCreditNoteStatusIn(companyId, purchase.getIdPurchase(), activeStatuses)) {
            throw new BusinessRuleException("Ya existe una nota de crédito activa (PENDIENTE o APLICADA) para esta compra.");
        }

        // 2. Validaciones de integridad financiera y de datos
        validateFinancialConsistency(dto.getDetails(), dto.getSubtotalAmount(), dto.getVatAmount(), dto.getTotalAmount());
        validateDetailsAgainstOriginalPurchase(dto.getDetails(), purchase);
        validateDuplicateItemsInDTO(dto.getDetails());

        // 3. Construcción de la entidad
        Company companyRef = companyRepository.getReferenceById(companyId);
        PurchaseCreditNote newCreditNote = new PurchaseCreditNote();
        newCreditNote.setCompany(companyRef);
        newCreditNote.setPurchase(purchase);
        newCreditNote.setDocumentNumber(dto.getDocumentNumber());
        newCreditNote.setDescription(dto.getDescription());
        newCreditNote.setIssueDate(dto.getIssueDate());
        newCreditNote.setCreditNoteStatus("PENDIENTE");
        newCreditNote.setSubtotalAmount(dto.getSubtotalAmount());
        newCreditNote.setVatAmount(dto.getVatAmount());
        newCreditNote.setTotalAmount(dto.getTotalAmount());

        for (PurchaseCreditNoteDetailCreateDTO detailDTO : dto.getDetails()) {
            newCreditNote.addDetail(mapToPurchaseCreditNoteDetail(detailDTO));
        }

        PurchaseCreditNote savedCreditNote = creditNoteRepository.save(newCreditNote);

        changeHistoryService.logChange("Notas de Crédito - Compras",
                String.format("Creó la nota de crédito N° %s por $%.2f, sobre la compra N° %s.",
                        savedCreditNote.getDocumentNumber(), savedCreditNote.getTotalAmount(), savedCreditNote.getPurchase().getDocumentNumber()));

        return modelMapper.map(savedCreditNote, PurchaseCreditNoteResponseDTO.class);
    }

    @Transactional
    public PurchaseCreditNoteResponseDTO updateCreditNote(Integer id, PurchaseCreditNoteUpdateDTO dto) {
        PurchaseCreditNote creditNote = creditNoteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nota de crédito de compra con ID " + id + " no encontrada."));

        if (!"PENDIENTE".equals(creditNote.getCreditNoteStatus())) {
            throw new BusinessRuleException("Solo se pueden editar notas de crédito en estado PENDIENTE.");
        }

        // Validar unicidad del número de documento si cambia
        if (dto.getDocumentNumber() != null && !dto.getDocumentNumber().equals(creditNote.getDocumentNumber()) &&
                creditNoteRepository.existsByCompany_IdAndDocumentNumber(creditNote.getCompany().getId(), dto.getDocumentNumber())) {
            throw new BusinessRuleException("El número de documento " + dto.getDocumentNumber() + " ya está en uso.");
        }

        // Actualizar campos de cabecera
        if (dto.getDocumentNumber() != null) creditNote.setDocumentNumber(dto.getDocumentNumber());
        if (dto.getDescription() != null) creditNote.setDescription(dto.getDescription());
        if (dto.getIssueDate() != null) creditNote.setIssueDate(dto.getIssueDate());

        // Si se envían detalles, se reconstruye la colección y se validan los totales
        if (dto.getDetails() != null) {
            if (dto.getDetails().isEmpty()) {
                throw new BusinessRuleException("La lista de detalles no puede estar vacía en una actualización.");
            }
            if (dto.getSubtotalAmount() == null || dto.getVatAmount() == null || dto.getTotalAmount() == null) {
                throw new BusinessRuleException("Si se modifican los detalles, se deben enviar los nuevos totales.");
            }

            validateFinancialConsistency(dto.getDetails(), dto.getSubtotalAmount(), dto.getVatAmount(), dto.getTotalAmount());
            validateDetailsAgainstOriginalPurchase(dto.getDetails(), creditNote.getPurchase());
            validateDuplicateItemsInDTO(dto.getDetails());

            // Patrón de reconstrucción de colección
            reconstructDetails(creditNote, dto.getDetails());

            // Actualizar totales en la entidad
            creditNote.setSubtotalAmount(dto.getSubtotalAmount());
            creditNote.setVatAmount(dto.getVatAmount());
            creditNote.setTotalAmount(dto.getTotalAmount());
        }

        PurchaseCreditNote updatedCreditNote = creditNoteRepository.save(creditNote);
        changeHistoryService.logChange("Notas de Crédito - Compras",
                String.format("Actualizó la nota de crédito N° %s.", updatedCreditNote.getDocumentNumber()));

        return modelMapper.map(updatedCreditNote, PurchaseCreditNoteResponseDTO.class);
    }

    @Transactional
    public void delete(Integer id) {
        PurchaseCreditNote creditNote = creditNoteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nota de crédito de compra con ID " + id + " no encontrada."));

        if (!"PENDIENTE".equals(creditNote.getCreditNoteStatus())) {
            throw new BusinessRuleException("Solo se pueden eliminar notas de crédito en estado PENDIENTE.");
        }

        changeHistoryService.logChange("Notas de Crédito - Compras",
                String.format("Eliminó la nota de crédito PENDIENTE N° %s.", creditNote.getDocumentNumber()));

        creditNoteRepository.delete(creditNote);
    }

    // =========================================================================================
    // == MÉTODOS DE ORQUESTACIÓN Y TRANSICIÓN DE ESTADO
    // =========================================================================================

    @Transactional
    public PurchaseCreditNoteResponseDTO applyCreditNote(Integer creditNoteId) {
        PurchaseCreditNote creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new NotFoundException("Nota de crédito de compra con ID " + creditNoteId + " no encontrada."));

        if (!"PENDIENTE".equals(creditNote.getCreditNoteStatus())) {
            throw new BusinessRuleException("Solo se pueden aplicar notas de crédito en estado PENDIENTE.");
        }

        // --- ORQUESTACIÓN DE INTEGRACIONES ---

        // 1. INVENTARIO: Disminuir el stock (es una devolución de compra).
        inventoryService.processPurchaseCreditNoteApplication(creditNote);

        // 2. CONTABILIDAD: Generar el asiento contable de la devolución.
        purchasesAccountingService.createEntriesForCreditNoteApplication(creditNote);

        // 3. CUENTAS POR PAGAR: --- ¡IMPLEMENTACIÓN FINAL! ---
        // Se actualiza el monto a pagar, disminuyéndolo.
        accountsPayableService.updatePayableAmount(
                creditNote.getPurchase().getIdPurchase(), // El ID de la compra original
                creditNote.getTotalAmount(),              // El monto de la NC a restar
                "APLICADA"                                // La operación que indica una resta
        );

        // 4. SALDO DEL PROVEEDOR: Actualizar el saldo directamente.
        Supplier supplier = creditNote.getPurchase().getSupplier();
        supplier.setCurrentBalance(supplier.getCurrentBalance().subtract(creditNote.getTotalAmount()));
        supplierRepository.save(supplier);

        // 5. Actualizar estado y persistir
        creditNote.setCreditNoteStatus("APLICADA");
        PurchaseCreditNote appliedCreditNote = creditNoteRepository.save(creditNote);

        changeHistoryService.logChange("Notas de Crédito - Compras",
                String.format("Aplicó la nota de crédito N° %s. Estado cambió a APLICADA.", appliedCreditNote.getDocumentNumber()));

        return modelMapper.map(appliedCreditNote, PurchaseCreditNoteResponseDTO.class);
    }

    @Transactional
    public PurchaseCreditNoteResponseDTO cancelCreditNote(Integer creditNoteId) {
        PurchaseCreditNote creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new NotFoundException("Nota de crédito de compra con ID " + creditNoteId + " no encontrada."));

        if (!"APLICADA".equals(creditNote.getCreditNoteStatus())) {
            throw new BusinessRuleException("Solo se pueden anular notas de crédito en estado APLICADA.");
        }

        // Se comprueba si la compra original tiene pagos activos. Si es así, se bloquea la anulación.
        boolean hasActivePayments = accountsPayableService.validatePurchaseWithoutCollections(creditNote.getPurchase().getIdPurchase());
        if (hasActivePayments) {
            throw new BusinessRuleException(
                    "No se puede anular la nota de crédito porque la compra asociada ya tiene pagos registrados. " +
                            "Por favor, anule primero los pagos en el módulo de Cuentas por Pagar."
            );
        }
        
        // --- ORQUESTACIÓN DE REVERSIONES ---

        // 1. INVENTARIO: Revertir la disminución de stock (es decir, AUMENTAR stock).
        inventoryService.processPurchaseCreditNoteCancellation(creditNote);

        // 2. CONTABILIDAD: Eliminar/Revertir el asiento contable de la devolución.
        purchasesAccountingService.deleteEntriesForCreditNoteCancellation(creditNote);

        // 3. CUENTAS POR PAGAR: --- ¡IMPLEMENTACIÓN FINAL! ---
        // Se actualiza el monto a pagar, restaurándolo (sumándolo de nuevo).
        accountsPayableService.updatePayableAmount(
                creditNote.getPurchase().getIdPurchase(), // El ID de la compra original
                creditNote.getTotalAmount(),              // El monto de la NC a sumar
                "ANULADA"                                 // La operación que indica una suma
        );

        // 4. SALDO DEL PROVEEDOR: Revertir la actualización del saldo.
        Supplier supplier = creditNote.getPurchase().getSupplier();
        supplier.setCurrentBalance(supplier.getCurrentBalance().add(creditNote.getTotalAmount()));
        supplierRepository.save(supplier);

        // 5. Actualizar estado y persistir
        creditNote.setCreditNoteStatus("ANULADA");
        PurchaseCreditNote cancelledCreditNote = creditNoteRepository.save(creditNote);

        changeHistoryService.logChange("Notas de Crédito - Compras",
                String.format("Anuló la nota de crédito N° %s. Estado cambió a ANULADA.", cancelledCreditNote.getDocumentNumber()));

        return modelMapper.map(cancelledCreditNote, PurchaseCreditNoteResponseDTO.class);
    }

    // =========================================================================================
    // == MÉTODOS DE BÚSQUEDA
    // =========================================================================================

    @Transactional(readOnly = true)
    public List<PurchaseCreditNoteResponseDTO> findAll(String sortBy) {
        Integer companyId = getCompanyIdFromContext();
        List<PurchaseCreditNote> creditNotes;

        // ¡Ahora llamamos a los métodos con JOIN FETCH!
        if ("status".equalsIgnoreCase(sortBy)) {
            creditNotes = creditNoteRepository.findAllWithDetailsByCompanyIdOrderByStatus(companyId);
        } else {
            creditNotes = creditNoteRepository.findAllWithDetailsByCompanyIdOrderByDate(companyId);
        }

        return creditNotes.stream()
                .map(cn -> modelMapper.map(cn, PurchaseCreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseCreditNoteResponseDTO findById(Integer id) {
        // ¡Ahora llamamos al método con JOIN FETCH!
        PurchaseCreditNote creditNote = creditNoteRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Nota de crédito de compra con ID " + id + " no encontrada."));

        // ModelMapper ahora recibirá un objeto completamente inicializado
        return modelMapper.map(creditNote, PurchaseCreditNoteResponseDTO.class);
    }

    /**
     * Busca notas de crédito por el ID de la compra asociada, DENTRO DE LA EMPRESA ACTUAL.
     */
    @Transactional(readOnly = true)
    public List<PurchaseCreditNoteResponseDTO> findByPurchaseId(Integer purchaseId) {
        Integer companyId = getCompanyIdFromContext();

        // Seguridad: Verificar que la compra pertenezca a la empresa actual.
        purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Compra con ID " + purchaseId + " no encontrada."));

        return creditNoteRepository.findByCompany_IdAndPurchase_IdPurchase(companyId, purchaseId).stream()
                .map(cn -> modelMapper.map(cn, PurchaseCreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }


    /**
     * Busca notas de crédito por su estado, DENTRO DE LA EMPRESA ACTUAL.
     */
    @Transactional(readOnly = true)
    public List<PurchaseCreditNoteResponseDTO> findByStatus(String status) {
        Integer companyId = getCompanyIdFromContext();
        return creditNoteRepository.findByCompany_IdAndCreditNoteStatus(companyId, status).stream()
                .map(cn -> modelMapper.map(cn, PurchaseCreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Busca notas de crédito por rango de fechas y opcionalmente por estado, DENTRO DE LA EMPRESA ACTUAL.
     */
    @Transactional(readOnly = true)
    public List<PurchaseCreditNoteResponseDTO> findByDateRangeAndStatus(LocalDate start, LocalDate end, String status) {
        Integer companyId = getCompanyIdFromContext();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        String finalStatus = (status != null && !status.isBlank()) ? status : null;

        return creditNoteRepository.findByCompanyIdAndDateRangeAndStatus(companyId, startDateTime, endDateTime, finalStatus).stream()
                .map(cn -> modelMapper.map(cn, PurchaseCreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }
    // =========================================================================================
    // == MÉTODOS PRIVADOS DE AYUDA Y VALIDACIÓN
    // =========================================================================================

    // Reemplaza tu método de validación existente con esta versión mejorada.
    private void validateFinancialConsistency(List<PurchaseCreditNoteDetailCreateDTO> details, BigDecimal subtotal, BigDecimal vat, BigDecimal total) {

        // Asumimos una tasa de IVA del 13%. ¡Es mejor definir esto como una constante o configuración!
        final BigDecimal VAT_RATE = new BigDecimal("0.13");

        BigDecimal calculatedSubtotalFromDetails = BigDecimal.ZERO;
        BigDecimal calculatedVatFromDetails = BigDecimal.ZERO;

        for (var detail : details) {
            // Validación de subtotal de línea (sin cambios)
            BigDecimal lineSubtotal = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
            if (lineSubtotal.compareTo(detail.getSubtotal()) != 0) {
                throw new BusinessRuleException("Inconsistencia en el subtotal del item: cálculo (precio*cantidad) no coincide con el subtotal enviado.");
            }
            calculatedSubtotalFromDetails = calculatedSubtotalFromDetails.add(detail.getSubtotal());

            // --- ¡NUEVA LÓGICA DE CÁLCULO DE IVA! ---
            // Si la línea está marcada con impuesto, calculamos su IVA y lo sumamos.
            if (Boolean.TRUE.equals(detail.getTax())) {
                BigDecimal lineVat = detail.getSubtotal().multiply(VAT_RATE);
                calculatedVatFromDetails = calculatedVatFromDetails.add(lineVat);
            }
        }

        // Ahora comparamos nuestros cálculos con los totales enviados en la cabecera.
        // Usamos setScale para manejar posibles diferencias de redondeo.
        if (calculatedSubtotalFromDetails.compareTo(subtotal) != 0) {
            throw new BusinessRuleException("El subtotal de la cabecera (" + subtotal + ") no coincide con la suma de los detalles (" + calculatedSubtotalFromDetails + ").");
        }

        if (calculatedVatFromDetails.setScale(2, RoundingMode.HALF_UP).compareTo(vat.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new BusinessRuleException("El IVA de la cabecera (" + vat + ") no coincide con el IVA calculado de los detalles (" + calculatedVatFromDetails.setScale(2, RoundingMode.HALF_UP) + ").");
        }

        if (subtotal.add(vat).compareTo(total) != 0) {
            throw new BusinessRuleException("Inconsistencia en los totales: Subtotal + IVA no es igual al Total.");
        }
    }

    private void validateDetailsAgainstOriginalPurchase(List<PurchaseCreditNoteDetailCreateDTO> creditNoteDetails, Purchase originalPurchase) {
        Map<Object, Integer> originalItemsMap = originalPurchase.getPurchaseDetails().stream()
                .collect(Collectors.toMap(
                        // La clave es el ID del producto o el ID del catálogo. Lógica correcta.
                        detail -> detail.getProduct() != null ? (Object)detail.getProduct().getIdProduct() : detail.getCatalog().getId(),
                        PurchaseDetail::getQuantity
                ));

        for (var cnDetail : creditNoteDetails) {
            Object key = cnDetail.getProductId() != null ? (Object)cnDetail.getProductId() : cnDetail.getCatalogId();

            if (key == null || (cnDetail.getProductId() != null && cnDetail.getCatalogId() != null)) {
                throw new BusinessRuleException("Un detalle de nota de crédito debe tener un 'productId' o un 'catalogId', pero no ambos o ninguno.");
            }
            if (!originalItemsMap.containsKey(key)) {
                throw new BusinessRuleException("El ítem con ID " + key + " no puede ser devuelto porque no existía en la compra original.");
            }
            int originalQty = originalItemsMap.get(key);
            if (cnDetail.getQuantity() > originalQty) {
                throw new BusinessRuleException("La cantidad a devolver para el ítem " + key + " (" + cnDetail.getQuantity() + ") excede la cantidad comprada originalmente (" + originalQty + ").");
            }
        }
    }

    private void validateDuplicateItemsInDTO(List<PurchaseCreditNoteDetailCreateDTO> details) {
        Set<Object> seenKeys = new HashSet<>();
        for (var detail : details) {
            Object key = detail.getProductId() != null ? (Object)detail.getProductId() : detail.getCatalogId();
            if (!seenKeys.add(key)) {
                throw new BusinessRuleException("La solicitud contiene detalles duplicados para el mismo producto o cuenta contable (ID: " + key + ").");
            }
        }
    }

    private PurchaseCreditNoteDetail mapToPurchaseCreditNoteDetail(PurchaseCreditNoteDetailCreateDTO dto) {
        PurchaseCreditNoteDetail detail = new PurchaseCreditNoteDetail();
        detail.setQuantity(dto.getQuantity());
        detail.setUnitPrice(dto.getUnitPrice());
        detail.setSubtotal(dto.getSubtotal());
        detail.setTax(dto.getTax());
        detail.setLineDescription(dto.getLineDescription());

        if (dto.getProductId() != null) {
            Product product = productService.findEntityById(dto.getProductId());
            detail.setProduct(product);
        } else { // catalogId no puede ser null por validaciones previas
            // CORRECCIÓN: Usamos CatalogService para obtener la entidad Catalog y la asignamos directamente.
            Catalog catalog = catalogService.findEntityById(dto.getCatalogId());
            detail.setCatalog(catalog);
        }
        return detail;
    }

    private void reconstructDetails(PurchaseCreditNote creditNote, List<PurchaseCreditNoteDetailCreateDTO> dtos) {
        Map<Object, PurchaseCreditNoteDetail> existingDetailsMap = creditNote.getDetails().stream()
                .collect(Collectors.toMap(
                        // CORRECCIÓN: La clave es el ID del producto o el ID del catálogo.
                        d -> d.getProduct() != null ? (Object) d.getProduct().getIdProduct() : d.getCatalog().getId(),
                        Function.identity()
                ));

        Set<PurchaseCreditNoteDetail> finalDetails = new HashSet<>();

        for (PurchaseCreditNoteDetailCreateDTO dto : dtos) {
            Object key = dto.getProductId() != null ? (Object)dto.getProductId() : dto.getCatalogId();

            PurchaseCreditNoteDetail existingDetail = existingDetailsMap.get(key);
            if (existingDetail == null) {
                throw new BusinessRuleException("No se pueden añadir nuevos detalles a una nota de crédito existente. El ítem con ID " + key + " es nuevo.");
            }

            existingDetail.setQuantity(dto.getQuantity());
            existingDetail.setUnitPrice(dto.getUnitPrice());
            existingDetail.setSubtotal(dto.getSubtotal());
            existingDetail.setTax(dto.getTax());
            existingDetail.setLineDescription(dto.getLineDescription());
            finalDetails.add(existingDetail);
        }

        creditNote.getDetails().clear();
        creditNote.getDetails().addAll(finalDetails);
    }
}