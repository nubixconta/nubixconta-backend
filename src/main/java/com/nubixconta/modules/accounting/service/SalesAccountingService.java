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
import java.util.stream.Collectors;

import com.nubixconta.modules.accounting.dto.AccountingEntryLineDTO;
import com.nubixconta.modules.accounting.dto.AccountingEntryResponseDTO;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.sales.entity.Customer;

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

    // --- INICIO DE CÓDIGO AÑADIDO ---
    /**
     * Obtiene el asiento contable formateado para una venta específica.
     * @param saleId El ID de la venta.
     * @return Un DTO con toda la información necesaria para la vista de React.
     */
    @Transactional(readOnly = true)
    public AccountingEntryResponseDTO getEntryForSale(Integer saleId) {
        // 1. Usar el método optimizado del repositorio para obtener todas las líneas del asiento.
        List<SaleEntry> entries = saleEntryRepository.findBySaleIdWithDetails(saleId);
        if (entries.isEmpty()) {
            throw new NotFoundException("No se encontró asiento contable para la venta con ID: " + saleId);
        }

        // 2. Extraer información común de la primera línea del asiento.
        SaleEntry firstEntry = entries.get(0);
        Sale sale = firstEntry.getSale();
        Customer customer = sale.getCustomer();

        // 3. Mapear cada entidad de asiento a su DTO de línea.
        List<AccountingEntryLineDTO> lines = entries.stream()
                .map(entry -> new AccountingEntryLineDTO(
                        entry.getCatalog().getEffectiveCode(),
                        entry.getCatalog().getEffectiveName(),
                        entry.getDebe(),
                        entry.getHaber()
                ))
                .collect(Collectors.toList());

        // 4. Calcular los totales del asiento.
        BigDecimal totalDebits = lines.stream().map(AccountingEntryLineDTO::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = lines.stream().map(AccountingEntryLineDTO::credit).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Construir y devolver el DTO de respuesta final.
        return new AccountingEntryResponseDTO(
                firstEntry.getId(),             // ID del asiento contable
                sale.getDocumentNumber(),       // Número del documento padre (venta)
                "Venta",                        // Tipo de documento
                sale.getSaleStatus(),           // Estado de la venta
                "Cliente",                      // Etiqueta del socio de negocio
                formatPartnerName(customer),
                firstEntry.getDate(),           // Fecha de generación del asiento
                sale.getSaleDescription() != null ? sale.getSaleDescription() : "",// Descripción de la transacción
                lines,                          // Lista de movimientos
                totalDebits,                    // Total de débitos
                totalCredits                    // Total de créditos
        );
    }

    /**
     * Obtiene el asiento contable formateado para una nota de crédito específica.
     * @param creditNoteId El ID de la nota de crédito.
     * @return Un DTO con toda la información necesaria para la vista de React.
     */
    @Transactional(readOnly = true)
    public AccountingEntryResponseDTO getEntryForCreditNote(Integer creditNoteId) {
        List<CreditNoteEntry> entries = creditNoteEntryRepository.findByCreditNoteIdWithDetails(creditNoteId);
        if (entries.isEmpty()) {
            throw new NotFoundException("No se encontró asiento contable para la nota de crédito con ID: " + creditNoteId);
        }

        CreditNoteEntry firstEntry = entries.get(0);
        CreditNote creditNote = firstEntry.getCreditNote();
        Customer customer = creditNote.getSale().getCustomer();

        List<AccountingEntryLineDTO> lines = entries.stream()
                .map(entry -> new AccountingEntryLineDTO(
                        entry.getCatalog().getEffectiveCode(),
                        entry.getCatalog().getEffectiveName(),
                        entry.getDebe(),
                        entry.getHaber()
                ))
                .collect(Collectors.toList());

        BigDecimal totalDebits = lines.stream().map(AccountingEntryLineDTO::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = lines.stream().map(AccountingEntryLineDTO::credit).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AccountingEntryResponseDTO(
                firstEntry.getId(),             // ID del asiento contable
                creditNote.getDocumentNumber(), // Número del documento padre (NC)
                "Nota de Crédito",              // Tipo de documento
                creditNote.getCreditNoteStatus(), // Estado de la NC
                "Cliente",                      // Etiqueta del socio de negocio
                formatPartnerName(customer), // <-- Usamos nuestro método seguro
                firstEntry.getDate(),           // Fecha de generación del asiento
                creditNote.getDescription() != null ? creditNote.getDescription() : "", // <-- Añadimos seguridad para la descripción también
                lines,                          // Lista de movimientos
                totalDebits,                    // Total de débitos
                totalCredits                    // Total de créditos
        );
    }
    // --- FIN DE CÓDIGO AÑADIDO ---


    /**
     * Método privado de ayuda para formatear el nombre del cliente de forma segura.
     * Evita que se añada " null" si el apellido no existe.
     * @param customer El objeto del cliente.
     * @return El nombre completo formateado.
     */
    private String formatPartnerName(Customer customer) {
        if (customer == null) {
            return ""; // Caso de seguridad
        }
        String firstName = customer.getCustomerName();
        String lastName = customer.getCustomerLastName();

        // Si el apellido es nulo o está en blanco, devuelve solo el nombre.
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        // Si no, devuelve nombre y apellido.
        return firstName + " " + lastName;
    }
}