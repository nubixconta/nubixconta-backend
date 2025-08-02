package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.entity.CreditNoteEntry;
import com.nubixconta.modules.accounting.entity.SaleEntry;
import com.nubixconta.modules.accounting.repository.CreditNoteEntryRepository;
import com.nubixconta.modules.accounting.repository.SaleEntryRepository;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.sales.entity.CreditNote;
import com.nubixconta.modules.sales.entity.CreditNoteDetail;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.entity.SaleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio especializado en la lógica contable del módulo de Ventas.
 * Crea y elimina los asientos contables para Ventas y Notas de Crédito.
 */
@Service
@RequiredArgsConstructor
public class SalesAccountingService {

    private final SaleEntryRepository saleEntryRepository;
    private final CreditNoteEntryRepository creditNoteEntryRepository;
    // --- ¡NUEVA INYECCIÓN! Depende del servicio de configuración central. ---
    private final AccountingConfigurationService configService;

    // --- LÓGICA PARA VENTAS ---

    @Transactional(propagation = Propagation.MANDATORY)
    public void createEntriesForSaleApplication(Sale sale) {

        // 1. --- ¡CAMBIO CRÍTICO! Obtener el contexto de la empresa desde la venta. ---
        Company company = sale.getCompany();
        if (company == null) {
            throw new BusinessRuleException("La venta no está asociada a ninguna empresa.");
        }
        Integer companyId = company.getId();

        // 2. --- ¡CAMBIO CRÍTICO! Usar el nuevo servicio de configuración. ---
        // Se busca el 'Catalog' (la activación), no la 'Account' directamente.
        Catalog vatCatalog = configService.findCatalogBySettingKey("VAT_DEBIT_ACCOUNT", companyId);
        Catalog customerCatalog = configService.findCatalogBySettingKey("DEFAULT_CUSTOMER_ACCOUNT", companyId);

        // ... El resto de la lógica de cálculo de montos permanece igual ...
        BigDecimal totalProductSubtotal = BigDecimal.ZERO;
        BigDecimal totalServiceSubtotal = BigDecimal.ZERO;
        BigDecimal totalVat = sale.getVatAmount();

        for (SaleDetail detail : sale.getSaleDetails()) {
            BigDecimal subtotal = detail.getUnitPrice().multiply(new BigDecimal(detail.getQuantity()));
            if (detail.getProduct() != null) {
                totalProductSubtotal = totalProductSubtotal.add(subtotal);
            } else {
                totalServiceSubtotal = totalServiceSubtotal.add(subtotal);
            }
        }

        List<SaleEntry> entries = new ArrayList<>();
        String description = sale.getSaleDescription();

        // 3. --- ¡CAMBIO CRÍTICO! El método helper ahora recibe un objeto 'Catalog'. ---
        if (totalProductSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            Catalog productIncomeCatalog = configService.findCatalogBySettingKey("PRODUCT_INCOME_ACCOUNT", companyId);
            entries.add(createSaleEntry(sale, productIncomeCatalog, BigDecimal.ZERO, totalProductSubtotal, description));
        }
        if (totalServiceSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            // Esta línea ahora solo se ejecuta si la venta REALMENTE contiene servicios.
            // Esto soluciona el bug para la Empresa B.
            Catalog serviceIncomeCatalog = configService.findCatalogBySettingKey("SERVICE_INCOME_ACCOUNT", companyId);
            entries.add(createSaleEntry(sale, serviceIncomeCatalog, BigDecimal.ZERO, totalServiceSubtotal, description));
        }
        if (totalVat != null && totalVat.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createSaleEntry(sale, vatCatalog, BigDecimal.ZERO, totalVat, description));
        }
        entries.add(createSaleEntry(sale, customerCatalog, sale.getTotalAmount(), BigDecimal.ZERO, description));

        validateSaleEntryTotals(entries);
        saleEntryRepository.saveAll(entries);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteEntriesForSaleCancellation(Sale sale) {
        saleEntryRepository.deleteBySale_SaleId(sale.getSaleId());
    }

    // --- LÓGICA PARA NOTAS DE CRÉDITO ---

    @Transactional(propagation = Propagation.MANDATORY)
    public void createEntriesForCreditNoteApplication(CreditNote creditNote) {
        // 1. Obtener contexto de la empresa.
        Company company = creditNote.getCompany();
        if (company == null) {
            throw new BusinessRuleException("La nota de crédito no está asociada a ninguna empresa.");
        }
        Integer companyId = company.getId();

        // 2. Usar el servicio de configuración.
        Catalog vatCatalog = configService.findCatalogBySettingKey("VAT_DEBIT_ACCOUNT", companyId);
        Catalog customerCatalog = configService.findCatalogBySettingKey("DEFAULT_CUSTOMER_ACCOUNT", companyId);

        // ... Lógica de cálculo de montos sin cambios ...
        BigDecimal totalProductSubtotal = BigDecimal.ZERO;
        BigDecimal totalServiceSubtotal = BigDecimal.ZERO;
        BigDecimal totalVat = creditNote.getVatAmount();

        for (CreditNoteDetail detail : creditNote.getDetails()) {
            BigDecimal subtotal = detail.getUnitPrice().multiply(new BigDecimal(detail.getQuantity()));
            if (detail.getProduct() != null) {
                totalProductSubtotal = totalProductSubtotal.add(subtotal);
            } else {
                totalServiceSubtotal = totalServiceSubtotal.add(subtotal);
            }
        }

        List<CreditNoteEntry> entries = new ArrayList<>();
        String description = creditNote.getDescription();

        // 3. El método helper recibe 'Catalog'.
        if (totalProductSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            Catalog productIncomeCatalog = configService.findCatalogBySettingKey("PRODUCT_INCOME_ACCOUNT", companyId);
            entries.add(createCreditNoteEntry(creditNote, productIncomeCatalog, totalProductSubtotal, BigDecimal.ZERO, description));
        }
        if (totalServiceSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            // De nuevo, esta línea solo se ejecuta si la NC contiene servicios.
            Catalog serviceIncomeCatalog = configService.findCatalogBySettingKey("SERVICE_INCOME_ACCOUNT", companyId);
            entries.add(createCreditNoteEntry(creditNote, serviceIncomeCatalog, totalServiceSubtotal, BigDecimal.ZERO, description));
        }
        if (totalVat != null && totalVat.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createCreditNoteEntry(creditNote, vatCatalog, totalVat, BigDecimal.ZERO, description));
        }
        entries.add(createCreditNoteEntry(creditNote, customerCatalog, BigDecimal.ZERO, creditNote.getTotalAmount(), description));

        validateCreditNoteEntryTotals(entries);
        creditNoteEntryRepository.saveAll(entries);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteEntriesForCreditNoteCancellation(CreditNote creditNote) {
        creditNoteEntryRepository.deleteByCreditNote_IdNotaCredit(creditNote.getIdNotaCredit());
    }

    // --- MÉTODOS PRIVADOS DE AYUDA (MODIFICADOS) ---

    // Este método ahora recibe un 'Catalog'
    private SaleEntry createSaleEntry(Sale sale, Catalog catalog, BigDecimal debe, BigDecimal haber, String description) {
        SaleEntry entry = new SaleEntry();
        entry.setSale(sale);
        entry.setCatalog(catalog); // <-- ¡CAMBIO IMPORTANTE!
        entry.setDebe(debe);
        entry.setHaber(haber);
        entry.setDescription(description);
        return entry;
    }

    private void validateSaleEntryTotals(List<SaleEntry> entries) {
        BigDecimal totalDebits = entries.stream().map(SaleEntry::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entries.stream().map(SaleEntry::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalStateException("Asiento de Venta descuadrado. Debe: " + totalDebits + ", Haber: " + totalCredits);
        }
    }

    // Este método ahora recibe un 'Catalog'
    private CreditNoteEntry createCreditNoteEntry(CreditNote creditNote, Catalog catalog, BigDecimal debe, BigDecimal haber, String description) {
        CreditNoteEntry entry = new CreditNoteEntry();
        entry.setCreditNote(creditNote);
        entry.setCatalog(catalog); // <-- ¡CAMBIO IMPORTANTE!
        entry.setDebe(debe);
        entry.setHaber(haber);
        entry.setDescription(description);
        return entry;
    }

    private void validateCreditNoteEntryTotals(List<CreditNoteEntry> entries) {
        BigDecimal totalDebits = entries.stream().map(CreditNoteEntry::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entries.stream().map(CreditNoteEntry::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalStateException("Asiento de Nota de Crédito descuadrado. Debe: " + totalDebits + ", Haber: " + totalCredits);
        }
    }
}