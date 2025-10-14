package com.nubixconta.modules.purchases.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.AccountsPayable.service.AccountsPayableService;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.service.CatalogService; // <-- ¡NUEVA DEPENDENCIA!
import com.nubixconta.modules.accounting.service.PurchasesAccountingService;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.service.InventoryService;
import com.nubixconta.modules.inventory.service.ProductService;
import com.nubixconta.modules.purchases.dto.purchases.*;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.modules.purchases.entity.PurchaseDetail;
import com.nubixconta.modules.purchases.entity.Supplier;
import com.nubixconta.modules.purchases.repository.PurchaseRepository;
import com.nubixconta.modules.purchases.repository.SupplierRepository;
import com.nubixconta.security.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    // Dependencias del módulo actual y módulos compartidos
    private final PurchaseRepository purchaseRepository;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final CatalogService catalogService; // <-- ¡NUEVA DEPENDENCIA!
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final ChangeHistoryService changeHistoryService;
    private final CompanyRepository companyRepository;
    private final SupplierRepository supplierRepository;
    private final PurchasesAccountingService purchasesAccountingService;
    private final AccountsPayableService accountsPayableService;

    // Helper para obtener el companyId de forma segura
    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

    /**
     * Retorna todas las compras, aplicando un ordenamiento específico.
     */
    public List<PurchaseResponseDTO> findAll(String sortBy) {
        Integer companyId = getCompanyIdFromContext();
        List<Purchase> purchases;

        if ("status".equalsIgnoreCase(sortBy)) {
            purchases = purchaseRepository.findAllByCompanyIdOrderByStatusAndIssueDate(companyId);
        } else {
            purchases = purchaseRepository.findByCompany_IdOrderByIssueDateDesc(companyId);
        }

        return purchases.stream()
                .map(purchase -> modelMapper.map(purchase, PurchaseResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Busca una compra por su ID.
     */
    public PurchaseResponseDTO findById(Integer id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compra con ID " + id + " no encontrada"));
        return modelMapper.map(purchase, PurchaseResponseDTO.class);
    }

    /**
     * Busca una compra por su estado.
     */
    public List<PurchaseResponseDTO> findByStatus(String status) {
        Integer companyId = getCompanyIdFromContext();
        List<Purchase> purchases = purchaseRepository.findByCompany_IdAndPurchaseStatus(companyId, status.toUpperCase());
        return purchases.stream()
                .map(purchase -> modelMapper.map(purchase, PurchaseResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Busca compras aplicadas utilizando una combinación de filtros para reportes.
     */
    public List<PurchaseResponseDTO> findByCombinedCriteria(LocalDate startDate, LocalDate endDate, String supplierName, String supplierLastName) {
        Integer companyId = getCompanyIdFromContext();
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        List<Purchase> purchases = purchaseRepository.findByCombinedCriteria(companyId, startDateTime, endDateTime, supplierName, supplierLastName);

        return purchases.stream()
                .map(purchase -> modelMapper.map(purchase, PurchaseResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Crea una nueva compra. La compra nace en estado 'PENDIENTE'.
     */
    @Transactional
    public PurchaseResponseDTO createPurchase(PurchaseCreateDTO dto) {
        Integer companyId = getCompanyIdFromContext();

        // --- VALIDACIONES DE NEGOCIO (RÉPLICA DE VENTAS) ---
        if (purchaseRepository.existsByCompany_IdAndDocumentNumber(companyId, dto.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe una compra con el número de documento: " + dto.getDocumentNumber());
        }
        if (dto.getPurchaseDetails().size() > 15) { // Límite de líneas por documento
            throw new BusinessRuleException("Una compra no puede tener más de 15 líneas de detalle.");
        }
        validateFinancialConsistency(dto.getPurchaseDetails(), dto.getSubtotalAmount(), dto.getVatAmount(), dto.getTotalAmount());
        validateDuplicateItemsInDTO(dto.getPurchaseDetails());

        // --- VALIDACIÓN DE CRÉDITO DEL PROVEEDOR ---
        Supplier supplier = supplierService.findEntityById(dto.getSupplierId());
        BigDecimal potentialNewBalance = supplier.getCurrentBalance().add(dto.getTotalAmount());
        if (potentialNewBalance.compareTo(supplier.getCreditLimit()) > 0) {
            throw new BusinessRuleException("Límite de crédito del proveedor excedido.");
        }

        // --- CONSTRUCCIÓN MANUAL DE LA ENTIDAD (PATRÓN SEGURO) ---
        Company companyRef = companyRepository.getReferenceById(companyId);
        Purchase newPurchase = new Purchase();
        newPurchase.setSupplier(supplier);
        newPurchase.setCompany(companyRef);
        newPurchase.setDocumentNumber(dto.getDocumentNumber());
        newPurchase.setPurchaseStatus("PENDIENTE");
        newPurchase.setIssueDate(dto.getIssueDate());
        newPurchase.setSubtotalAmount(dto.getSubtotalAmount());
        newPurchase.setVatAmount(dto.getVatAmount());
        newPurchase.setTotalAmount(dto.getTotalAmount());
        newPurchase.setPurchaseDescription(dto.getPurchaseDescription());
        newPurchase.setModuleType(dto.getModuleType());

        // Procesar y añadir detalles
        Set<PurchaseDetail> details = mapDetailsToEntities(dto.getPurchaseDetails(), newPurchase);
        newPurchase.setPurchaseDetails(details);

        Purchase savedPurchase = purchaseRepository.save(newPurchase);

        // --- REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Creó la compra con documento '%s' por un total de $%.2f.", savedPurchase.getDocumentNumber(), savedPurchase.getTotalAmount());
        changeHistoryService.logChange("Compras", logMessage);

        return modelMapper.map(savedPurchase, PurchaseResponseDTO.class);
    }

    // AGREGAR ESTE CÓDIGO DENTRO DE LA CLASE PurchaseService

    /**
     * Actualiza una compra existente de forma parcial.
     * Solo se permite si la compra está en estado 'PENDIENTE'.
     * Replica la lógica de 'updateSalePartial' para manejar la actualización,
     * adición y eliminación de líneas de detalle (productos o gastos).
     *
     * @param id El ID de la compra a actualizar.
     * @param dto El DTO con los campos a modificar.
     * @return El DTO de la compra actualizada.
     */
    @Transactional
    public PurchaseResponseDTO updatePurchase(Integer id, PurchaseUpdateDTO dto) {
        // 1. Obtener el contexto de la empresa.
        Integer companyId = getCompanyIdFromContext();

        // 2. Buscar la compra que vamos a actualizar.
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compra con ID " + id + " no encontrada para actualizar."));

        // 3. REGLA DE NEGOCIO: Solo se pueden editar compras PENDIENTES.
        if (!"PENDIENTE".equals(purchase.getPurchaseStatus())) {
            throw new BusinessRuleException("Solo se pueden editar compras con estado PENDIENTE. Estado actual: " + purchase.getPurchaseStatus());
        }

        // 4. Validar unicidad del número de documento si se está cambiando.
        if (dto.getDocumentNumber() != null &&
                !dto.getDocumentNumber().equals(purchase.getDocumentNumber()) &&
                purchaseRepository.existsByCompany_IdAndDocumentNumber(companyId, dto.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe otra compra con el número de documento: " + dto.getDocumentNumber());
        }

        // 5. Si se envían detalles, se deben realizar todas las validaciones.
        if (dto.getPurchaseDetails() != null) {
            // 5.1. Límite de líneas por documento.
            if (dto.getPurchaseDetails().size() > 15) {
                throw new BusinessRuleException("Una compra no puede tener más de 15 líneas de detalle.");
            }
            // 5.2. Evitar items duplicados (productos o cuentas) en la solicitud.
            validateDuplicateItemsInDTO(dto.getPurchaseDetails());

            // 5.3. Validar consistencia financiera completa.
            if (dto.getSubtotalAmount() == null || dto.getVatAmount() == null || dto.getTotalAmount() == null) {
                throw new BusinessRuleException("Si se modifican los detalles, se deben enviar los nuevos valores de subtotalAmount, vatAmount y totalAmount.");
            }
            validateFinancialConsistency(dto.getPurchaseDetails(), dto.getSubtotalAmount(), dto.getVatAmount(), dto.getTotalAmount());
        }

        // 6. Actualizar campos simples de la cabecera de la compra MANUALMENTE.
        if (dto.getDocumentNumber() != null) purchase.setDocumentNumber(dto.getDocumentNumber());
        if (dto.getIssueDate() != null) purchase.setIssueDate(dto.getIssueDate());
        if (dto.getPurchaseDescription() != null) purchase.setPurchaseDescription(dto.getPurchaseDescription());

        // Actualizar totales solo si se proporcionaron (podrían cambiar sin cambiar detalles).
        if (dto.getSubtotalAmount() != null) purchase.setSubtotalAmount(dto.getSubtotalAmount());
        if (dto.getVatAmount() != null) purchase.setVatAmount(dto.getVatAmount());
        if (dto.getTotalAmount() != null) purchase.setTotalAmount(dto.getTotalAmount());

        // 7. Lógica de sincronización de detalles (si se proporcionan).
        if (dto.getPurchaseDetails() != null) {
            // Creamos un mapa de los detalles existentes para fácil acceso.
            // La clave es el ID del producto o el ID del catálogo.
            Map<Object, PurchaseDetail> existingDetailsMap = purchase.getPurchaseDetails().stream()
                    .collect(Collectors.toMap(
                            detail -> detail.getProduct() != null ? (Object) detail.getProduct().getIdProduct() : detail.getCatalog().getId(),
                            detail -> detail
                    ));

            Set<PurchaseDetail> updatedDetails = new HashSet<>();

            for (PurchaseDetailCreateDTO detailDTO : dto.getPurchaseDetails()) {
                Object key = detailDTO.getProductId() != null ? (Object) detailDTO.getProductId() : detailDTO.getCatalogId();
                PurchaseDetail existingDetail = existingDetailsMap.get(key);

                if (existingDetail != null) {
                    // DETALLE EXISTENTE: Actualizamos sus valores.
                    existingDetail.setQuantity(detailDTO.getQuantity());
                    existingDetail.setUnitPrice(detailDTO.getUnitPrice());
                    existingDetail.setSubtotal(detailDTO.getSubtotal());
                    existingDetail.setTax(detailDTO.getTax());
                    existingDetail.setLineDescription(detailDTO.getLineDescription());
                    updatedDetails.add(existingDetail);
                    existingDetailsMap.remove(key); // Lo removemos para saber que ya fue procesado.
                } else {
                    // NUEVO DETALLE: Lo creamos y asociamos a la compra.
                    PurchaseDetail newDetail = mapToPurchaseDetail(detailDTO, purchase);
                    updatedDetails.add(newDetail);
                }
            }

            // PATRÓN DE RECONSTRUCCIÓN:
            // Limpiamos la colección actual y añadimos la nueva colección actualizada.
            // Gracias a `orphanRemoval = true`, JPA/Hibernate eliminará de la BD
            // los detalles que quedaron en `existingDetailsMap` (los que no vinieron en el DTO).
            purchase.getPurchaseDetails().clear();
            purchase.getPurchaseDetails().addAll(updatedDetails);
        }

        // 8. Guardar la compra actualizada.
        Purchase updatedPurchase = purchaseRepository.save(purchase);

        // --- REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Actualizó la compra con documento '%s'.", updatedPurchase.getDocumentNumber());
        changeHistoryService.logChange("Compras", logMessage);

        return modelMapper.map(updatedPurchase, PurchaseResponseDTO.class);
    }

    /**
     * Método de ayuda para mapear un DTO de detalle a una entidad PurchaseDetail.
     * Es una versión refactorizada de la lógica en `mapDetailsToEntities` para un solo objeto.
     */
    private PurchaseDetail mapToPurchaseDetail(PurchaseDetailCreateDTO dto, Purchase purchase) {
        // Validación de exclusividad
        if ((dto.getProductId() != null && dto.getCatalogId() != null) || (dto.getProductId() == null && dto.getCatalogId() == null)) {
            throw new BusinessRuleException("Un detalle debe ser un producto o un gasto contable, pero no ambos o ninguno.");
        }

        PurchaseDetail detail = new PurchaseDetail();
        detail.setPurchase(purchase);
        detail.setQuantity(dto.getQuantity());
        detail.setUnitPrice(dto.getUnitPrice());
        detail.setSubtotal(dto.getSubtotal());
        detail.setTax(dto.getTax());
        detail.setLineDescription(dto.getLineDescription());

        if (dto.getProductId() != null) {
            Product product = productService.findEntityById(dto.getProductId());
            detail.setProduct(product);
        } else { // catalogId no es null
            Catalog catalog = catalogService.findEntityById(dto.getCatalogId());
            if (!catalog.getCompany().getId().equals(purchase.getCompany().getId())) {
                throw new BusinessRuleException("Error de consistencia: La cuenta contable '" + catalog.getAccount().getAccountName() + "' no pertenece a la empresa de la compra.");
            }
            detail.setCatalog(catalog);
        }
        return detail;
    }

    /**
     * Elimina una compra. Solo permitido si está en estado 'PENDIENTE'.
     */
    @Transactional
    public void deletePurchase(Integer id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compra con ID " + id + " no encontrada para eliminar."));

        if (!"PENDIENTE".equals(purchase.getPurchaseStatus())) {
            throw new BusinessRuleException("Solo se pueden eliminar compras con estado PENDIENTE.");
        }

        String logMessage = String.format("Eliminó la compra PENDIENTE con documento '%s'.", purchase.getDocumentNumber());
        changeHistoryService.logChange("Compras", logMessage);

        purchaseRepository.delete(purchase);
    }

    /**
     * Aplica una compra. Cambia el estado a 'APLICADA' y dispara las interacciones con otros módulos.
     */
    @Transactional
    public PurchaseResponseDTO applyPurchase(Integer purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Compra con ID " + purchaseId + " no encontrada."));

        if (!"PENDIENTE".equals(purchase.getPurchaseStatus())) {
            throw new BusinessRuleException("La compra solo puede ser aplicada si su estado es PENDIENTE.");
        }

        // --- VALIDACIONES ANTES DE APLICAR ---
        validateActiveProductsInDetails(purchase);
        Supplier supplier = purchase.getSupplier();
        BigDecimal newBalance = supplier.getCurrentBalance().add(purchase.getTotalAmount());
        if (newBalance.compareTo(supplier.getCreditLimit()) > 0) {
            throw new BusinessRuleException("Límite de crédito del proveedor excedido.");
        }

        // 1. Afectar Inventario: Aumentar stock para productos.
        inventoryService.processPurchaseApplication(purchase);

        // 2. Generar Asiento Contable
        purchasesAccountingService.createEntriesForPurchaseApplication(purchase);

        // 3. Crear la Cuenta por Pagar correspondiente a esta compra.
        // La transacción se asegurará de que si esto falla, todo lo anterior se revierta.
        accountsPayableService.findOrCreateAccountsPayable(purchase);

        // --- ACTUALIZACIÓN DE ESTADOS ---
        supplier.setCurrentBalance(newBalance);
        purchase.setPurchaseStatus("APLICADA");

        supplierRepository.save(supplier);
        Purchase appliedPurchase = purchaseRepository.save(purchase);

        String logMessage = String.format("Aplicó la compra con documento '%s'.", appliedPurchase.getDocumentNumber());
        changeHistoryService.logChange("Compras", logMessage);

        return modelMapper.map(appliedPurchase, PurchaseResponseDTO.class);
    }

    /**
     * Anula una compra. Cambia el estado a 'ANULADA' y revierte las interacciones.
     */
    @Transactional
    public PurchaseResponseDTO cancelPurchase(Integer purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Compra con ID " + purchaseId + " no encontrada."));

        if (!"APLICADA".equals(purchase.getPurchaseStatus())) {
            throw new BusinessRuleException("La compra solo puede ser anulada si su estado es APLICADA.");
        }

        // --- VALIDACIÓN DE INTEGRIDAD (PLACEHOLDER) ---
        // TODO: 1. Validar que la compra no tenga pagos registrados
        // boolean hasPayments = accountsPayableService.validatePurchaseHasNoPayments(purchaseId);
        // if (hasPayments) {
        //     throw new BusinessRuleException("No se puede anular la compra porque tiene pagos asociados.");
        // }

        // --- REVERSIÓN EN OTROS MÓDULOS (PLACEHOLDERS) ---
        // 2. Revertir Inventario: Disminuir stock para productos.
        inventoryService.processPurchaseCancellation(purchase);

        // 3. Revertir Asiento Contable
        purchasesAccountingService.deleteEntriesForPurchaseCancellation(purchase);

        // TODO: 4. Revertir Cuenta por Pagar
        // accountsPayableService.cancelPayableForPurchase(purchase);

        // --- ACTUALIZACIÓN DE ESTADOS ---
        Supplier supplier = purchase.getSupplier();
        BigDecimal revertedBalance = supplier.getCurrentBalance().subtract(purchase.getTotalAmount());
        supplier.setCurrentBalance(revertedBalance);
        purchase.setPurchaseStatus("ANULADA");

        supplierRepository.save(supplier);
        Purchase cancelledPurchase = purchaseRepository.save(purchase);

        String logMessage = String.format("Anuló la compra con documento '%s'.", cancelledPurchase.getDocumentNumber());
        changeHistoryService.logChange("Compras", logMessage);

        return modelMapper.map(cancelledPurchase, PurchaseResponseDTO.class);
    }

    // --- MÉTODOS PRIVADOS DE VALIDACIÓN Y AYUDA ---

    private void validateFinancialConsistency(List<PurchaseDetailCreateDTO> details, BigDecimal subtotal, BigDecimal vat, BigDecimal total) {
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        for (PurchaseDetailCreateDTO detail : details) {
            BigDecimal lineSubtotal = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
            if (lineSubtotal.compareTo(detail.getSubtotal()) != 0) {
                throw new BusinessRuleException("Inconsistencia en el subtotal del item: " + detail.getLineDescription());
            }
            calculatedSubtotal = calculatedSubtotal.add(detail.getSubtotal());
        }
        if (calculatedSubtotal.compareTo(subtotal) != 0) {
            throw new BusinessRuleException("El subtotal de la cabecera no coincide con la suma de los detalles.");
        }
        if (subtotal.add(vat).compareTo(total) != 0) {
            throw new BusinessRuleException("Inconsistencia en los totales: Subtotal + IVA no es igual al Total.");
        }
    }

    private void validateDuplicateItemsInDTO(List<PurchaseDetailCreateDTO> details) {
        Set<Integer> seenProductIds = new HashSet<>();
        Set<Integer> seenCatalogIds = new HashSet<>();
        for (PurchaseDetailCreateDTO detail : details) {
            if (detail.getProductId() != null) {
                if (!seenProductIds.add(detail.getProductId())) {
                    throw new BusinessRuleException("Producto duplicado en los detalles: ID " + detail.getProductId());
                }
            } else if (detail.getCatalogId() != null) {
                if (!seenCatalogIds.add(detail.getCatalogId())) {
                    throw new BusinessRuleException("Cuenta contable duplicada en los detalles: ID Catálogo " + detail.getCatalogId());
                }
            }
        }
    }

    private Set<PurchaseDetail> mapDetailsToEntities(List<PurchaseDetailCreateDTO> detailDTOs, Purchase purchase) {
        return detailDTOs.stream().map(dto -> {
            // Validación de exclusividad
            if (dto.getProductId() != null && dto.getCatalogId() != null) {
                throw new BusinessRuleException("Un detalle no puede ser un producto y un gasto a la vez.");
            }
            if (dto.getProductId() == null && dto.getCatalogId() == null) {
                throw new BusinessRuleException("Un detalle debe ser un producto o un gasto contable.");
            }

            PurchaseDetail detail = new PurchaseDetail();
            detail.setPurchase(purchase);
            detail.setQuantity(dto.getQuantity());
            detail.setUnitPrice(dto.getUnitPrice());
            detail.setSubtotal(dto.getSubtotal());
            detail.setTax(dto.getTax());
            detail.setLineDescription(dto.getLineDescription());

            if (dto.getProductId() != null) {
                Product product = productService.findEntityById(dto.getProductId());
                detail.setProduct(product);
            } else {
                Catalog catalog = catalogService.findEntityById(dto.getCatalogId());
                // Validación de consistencia multi-tenant
                if (!catalog.getCompany().getId().equals(purchase.getCompany().getId())) {
                    throw new BusinessRuleException("La cuenta contable seleccionada no pertenece a la empresa de la compra.");
                }
                detail.setCatalog(catalog);
            }
            return detail;
        }).collect(Collectors.toSet());
    }

    private void validateActiveProductsInDetails(Purchase purchase) {
        List<String> inactiveProductNames = purchase.getPurchaseDetails().stream()
                .filter(detail -> detail.getProduct() != null && !detail.getProduct().getProductStatus())
                .map(detail -> detail.getProduct().getProductName())
                .collect(Collectors.toList());

        if (!inactiveProductNames.isEmpty()) {
            throw new BusinessRuleException("No se puede aplicar la compra. Los siguientes productos están inactivos: " + String.join(", ", inactiveProductNames));
        }
    }
    /**
     * Busca el ID de una compra dado su número de documento.
     *
     * @param documentNumber El número de documento de la compra.
     * @return El idPurchase.
     */
    public Integer findIdByDocumentNumber(String documentNumber) {
        Integer companyId = getCompanyIdFromContext();

        Purchase purchase = purchaseRepository.findByCompany_IdAndDocumentNumber(companyId, documentNumber)
                .orElseThrow(() -> new NotFoundException("Compra con número de documento " + documentNumber + " no encontrada."));

        return purchase.getIdPurchase();
    }

}