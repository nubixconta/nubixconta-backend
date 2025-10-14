package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.accounting.dto.AccountingEntryLineDTO;
import com.nubixconta.modules.accounting.dto.AccountingEntryResponseDTO;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.entity.PurchaseCreditNoteEntry;
import com.nubixconta.modules.accounting.entity.PurchaseEntry;
import com.nubixconta.modules.accounting.repository.PurchaseCreditNoteEntryRepository;
import com.nubixconta.modules.accounting.repository.PurchaseEntryRepository;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.modules.purchases.entity.PurchaseCreditNote;
import com.nubixconta.modules.purchases.entity.PurchaseDetail;
import com.nubixconta.modules.purchases.entity.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchasesAccountingService {

    private final PurchaseEntryRepository purchaseEntryRepository;
    private final AccountingConfigurationService configService;
    private final PurchaseCreditNoteEntryRepository purchaseCreditNoteEntryRepository;

    /**
     * Crea la partida contable completa para una compra que se está aplicando.
     * Sigue la lógica de partida doble discutida.
     * @param purchase La entidad completa de la compra.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createEntriesForPurchaseApplication(Purchase purchase) {
        Company company = purchase.getCompany();
        Integer companyId = company.getId();
        String description = "Registro de compra s/g doc: " + purchase.getDocumentNumber();

        // 1. Obtener las cuentas FIJAS desde la configuración contable.
        Catalog inventoryCatalog = configService.findCatalogBySettingKey("INVENTORY_ASSET_ACCOUNT", companyId);
        Catalog vatCreditCatalog = configService.findCatalogBySettingKey("VAT_CREDIT_ACCOUNT", companyId);
        Catalog supplierCatalog = configService.findCatalogBySettingKey("DEFAULT_SUPPLIER_ACCOUNT", companyId);

        List<PurchaseEntry> entries = new ArrayList<>();

        // 2. Procesar líneas de detalle para separar productos de gastos.
        BigDecimal totalProductSubtotal = BigDecimal.ZERO;

        for (PurchaseDetail detail : purchase.getPurchaseDetails()) {
            if (detail.getProduct() != null) {
                // Acumulamos el subtotal de todos los productos para un solo cargo.
                totalProductSubtotal = totalProductSubtotal.add(detail.getSubtotal());
            } else if (detail.getCatalog() != null) {
                // Para gastos, creamos una línea de CARGO (DEBE) inmediatamente a su cuenta específica.
                entries.add(createPurchaseEntry(purchase, detail.getCatalog(), detail.getSubtotal(), BigDecimal.ZERO, description));
            }
        }

        // 3. Crear las líneas de CARGO (DEBE) para las cuentas fijas.
        // Cargo único para la suma de todos los productos al inventario.
        if (totalProductSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createPurchaseEntry(purchase, inventoryCatalog, totalProductSubtotal, BigDecimal.ZERO, description));
        }

        // Cargo único para el total del IVA.
        if (purchase.getVatAmount().compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createPurchaseEntry(purchase, vatCreditCatalog, purchase.getVatAmount(), BigDecimal.ZERO, description));
        }

        // 4. Crear la línea de ABONO (HABER) para el proveedor (la contrapartida).
        entries.add(createPurchaseEntry(purchase, supplierCatalog, BigDecimal.ZERO, purchase.getTotalAmount(), description));

        // 5. Validar que la partida esté cuadrada y guardarla.
        validatePurchaseEntryTotals(entries);
        purchaseEntryRepository.saveAll(entries);
    }

    /**
     * Elimina la partida contable asociada a una compra que se está anulando.
     * @param purchase La entidad de la compra.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteEntriesForPurchaseCancellation(Purchase purchase) {
        purchaseEntryRepository.deleteByPurchase_IdPurchase(purchase.getIdPurchase());
    }

    /**
     * Obtiene el asiento contable formateado para una compra específica.
     * @param purchaseId El ID de la compra.
     * @return Un AccountingEntryResponseDTO con toda la información para el frontend.
     */
    @Transactional(readOnly = true)
    public AccountingEntryResponseDTO getEntryForPurchase(Integer purchaseId) {
        // 1. Usar el método optimizado del repositorio para obtener todas las líneas.
        List<PurchaseEntry> entries = purchaseEntryRepository.findByPurchaseIdWithDetails(purchaseId);
        if (entries.isEmpty()) {
            throw new NotFoundException("No se encontró asiento contable para la compra con ID: " + purchaseId);
        }

        // 2. Extraer información común de la primera línea del asiento.
        PurchaseEntry firstEntry = entries.get(0);
        Purchase purchase = firstEntry.getPurchase();
        Supplier supplier = purchase.getSupplier();

        // 3. Mapear cada entidad de asiento a su DTO de línea universal.
        List<AccountingEntryLineDTO> lines = entries.stream()
                .map(entry -> new AccountingEntryLineDTO(
                        entry.getCatalog().getAccount().getGeneratedCode(),
                        entry.getCatalog().getAccount().getAccountName(),
                        entry.getDebe(),
                        entry.getHaber()
                ))
                .collect(Collectors.toList());

        // 4. Calcular los totales del asiento.
        BigDecimal totalDebits = lines.stream().map(AccountingEntryLineDTO::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = lines.stream().map(AccountingEntryLineDTO::credit).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Construir y devolver el DTO de respuesta final y universal.
        return new AccountingEntryResponseDTO(
                firstEntry.getId(),
                purchase.getDocumentNumber(),
                "Compra", // Tipo de documento
                purchase.getPurchaseStatus(),
                "Proveedor", // Etiqueta del socio de negocio
                formatPartnerName(supplier),
                firstEntry.getDate(),
                purchase.getPurchaseDescription(),
                lines,
                totalDebits,
                totalCredits
        );
    }
    /**
     * Crea la partida contable para una Nota de Crédito sobre Compra que se está aplicando.
     * Lógica: Invierte el asiento de la compra original.
     * @param creditNote La entidad completa de la nota de crédito.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createEntriesForCreditNoteApplication(PurchaseCreditNote creditNote) {
        Company company = creditNote.getCompany();
        Integer companyId = company.getId();
        String description = "Devolución de compra s/g NC: " + creditNote.getDocumentNumber();

        // 1. Obtener las cuentas FIJAS desde la configuración contable.
        Catalog supplierCatalog = configService.findCatalogBySettingKey("DEFAULT_SUPPLIER_ACCOUNT", companyId);
        Catalog inventoryCatalog = configService.findCatalogBySettingKey("INVENTORY_ASSET_ACCOUNT", companyId);
        Catalog vatCreditCatalog = configService.findCatalogBySettingKey("VAT_CREDIT_ACCOUNT", companyId);

        List<PurchaseCreditNoteEntry> entries = new ArrayList<>();

        // 2. Crear la línea de CARGO (DEBE) al proveedor (disminuye la deuda).
        entries.add(createPurchaseCreditNoteEntry(creditNote, supplierCatalog, creditNote.getTotalAmount(), BigDecimal.ZERO, description));

        // 3. Procesar líneas de detalle para los ABONOS (HABER).
        BigDecimal totalProductSubtotal = BigDecimal.ZERO;

        for (var detail : creditNote.getDetails()) {
            if (detail.getProduct() != null) {
                totalProductSubtotal = totalProductSubtotal.add(detail.getSubtotal());
            } else if (detail.getCatalog() != null) {
                // Para gastos devueltos, creamos una línea de ABONO (HABER) a su cuenta específica.
                entries.add(createPurchaseCreditNoteEntry(creditNote, detail.getCatalog(), BigDecimal.ZERO, detail.getSubtotal(), description));
            }
        }

        // 4. Crear las líneas de ABONO (HABER) para las cuentas fijas.
        // Abono único para la suma de todos los productos devueltos del inventario.
        if (totalProductSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createPurchaseCreditNoteEntry(creditNote, inventoryCatalog, BigDecimal.ZERO, totalProductSubtotal, description));
        }

        // Abono único para el total del IVA devuelto.
        if (creditNote.getVatAmount().compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createPurchaseCreditNoteEntry(creditNote, vatCreditCatalog, BigDecimal.ZERO, creditNote.getVatAmount(), description));
        }

        // 5. Validar que la partida esté cuadrada y guardarla.
        validatePurchaseCreditNoteEntryTotals(entries);
        purchaseCreditNoteEntryRepository.saveAll(entries);
    }

    /**
     * Elimina la partida contable asociada a una Nota de Crédito sobre Compra que se está anulando.
     * @param creditNote La entidad de la nota de crédito.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteEntriesForCreditNoteCancellation(PurchaseCreditNote creditNote) {
        purchaseCreditNoteEntryRepository.deleteByPurchaseCreditNote_Id(creditNote.getId());
    }


    private PurchaseCreditNoteEntry createPurchaseCreditNoteEntry(PurchaseCreditNote creditNote, Catalog catalog, BigDecimal debe, BigDecimal haber, String description) {
        PurchaseCreditNoteEntry entry = new PurchaseCreditNoteEntry();
        entry.setPurchaseCreditNote(creditNote);
        entry.setCatalog(catalog);
        entry.setDebe(debe);
        entry.setHaber(haber);
        entry.setDescription(description);
        return entry;
    }

    private void validatePurchaseCreditNoteEntryTotals(List<PurchaseCreditNoteEntry> entries) {
        BigDecimal totalDebits = entries.stream().map(PurchaseCreditNoteEntry::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entries.stream().map(PurchaseCreditNoteEntry::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.setScale(2).compareTo(totalCredits.setScale(2)) != 0) {
            throw new BusinessRuleException("Asiento de Nota de Crédito de Compra descuadrado. Debe: " + totalDebits + ", Haber: " + totalCredits);
        }
    }


    /**
     * Método privado de ayuda para formatear el nombre del proveedor de forma segura.
     */
    private String formatPartnerName(Supplier supplier) {
        if (supplier == null) return "";
        // Reutilizamos el método que ya creamos en la entidad Supplier
        return supplier.getFullName();
    }

    // --- MÉTODOS PRIVADOS DE AYUDA ---

    private PurchaseEntry createPurchaseEntry(Purchase purchase, Catalog catalog, BigDecimal debe, BigDecimal haber, String description) {
        PurchaseEntry entry = new PurchaseEntry();
        entry.setPurchase(purchase);
        entry.setCatalog(catalog);
        entry.setDebe(debe);
        entry.setHaber(haber);
        entry.setDescription(description);
        return entry;
    }

    private void validatePurchaseEntryTotals(List<PurchaseEntry> entries) {
        BigDecimal totalDebits = entries.stream().map(PurchaseEntry::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entries.stream().map(PurchaseEntry::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Usamos scale de 2 para la comparación para evitar problemas de precisión
        if (totalDebits.setScale(2).compareTo(totalCredits.setScale(2)) != 0) {
            throw new BusinessRuleException("Asiento de Compra descuadrado. Debe: " + totalDebits + ", Haber: " + totalCredits);
        }
    }
}