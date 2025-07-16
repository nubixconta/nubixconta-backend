package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.entity.AccountingSetting;
import com.nubixconta.modules.accounting.entity.CreditNoteEntry;
import com.nubixconta.modules.accounting.entity.SaleEntry;
import com.nubixconta.modules.accounting.repository.AccountingSettingRepository;
import com.nubixconta.modules.accounting.repository.SaleEntryRepository;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.entity.SaleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.nubixconta.modules.accounting.repository.CreditNoteEntryRepository; // <-- ¡Nueva Inyección!
import com.nubixconta.modules.sales.entity.CreditNote; // <-- ¡Nueva Inyección!
import com.nubixconta.modules.sales.entity.CreditNoteDetail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final SaleEntryRepository saleEntryRepository;
    private final CreditNoteEntryRepository creditNoteEntryRepository;
    private final AccountingSettingRepository settingRepository;

    // --- LÓGICA PARA VENTAS ---

    @Transactional(propagation = Propagation.MANDATORY)
    public void createEntriesForSaleApplication(Sale sale) {
        Account productIncomeAccount = findAccountBySettingKey("PRODUCT_INCOME_ACCOUNT");
        Account serviceIncomeAccount = findAccountBySettingKey("SERVICE_INCOME_ACCOUNT");
        Account vatAccount = findAccountBySettingKey("VAT_DEBIT_ACCOUNT");
        Account customerAccount = findAccountBySettingKey("DEFAULT_CUSTOMER_ACCOUNT");

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

        if (totalProductSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createSaleEntry(sale, productIncomeAccount, BigDecimal.ZERO, totalProductSubtotal, description));
        }
        if (totalServiceSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createSaleEntry(sale, serviceIncomeAccount, BigDecimal.ZERO, totalServiceSubtotal, description));
        }
        if (totalVat != null && totalVat.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createSaleEntry(sale, vatAccount, BigDecimal.ZERO, totalVat, description));
        }
        entries.add(createSaleEntry(sale, customerAccount, sale.getTotalAmount(), BigDecimal.ZERO, description));

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
        Account productIncomeAccount = findAccountBySettingKey("PRODUCT_INCOME_ACCOUNT");
        Account serviceIncomeAccount = findAccountBySettingKey("SERVICE_INCOME_ACCOUNT");
        Account vatAccount = findAccountBySettingKey("VAT_DEBIT_ACCOUNT");
        Account customerAccount = findAccountBySettingKey("DEFAULT_CUSTOMER_ACCOUNT");

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

        if (totalProductSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createCreditNoteEntry(creditNote, productIncomeAccount, totalProductSubtotal, BigDecimal.ZERO, description));
        }
        if (totalServiceSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createCreditNoteEntry(creditNote, serviceIncomeAccount, totalServiceSubtotal, BigDecimal.ZERO, description));
        }
        if (totalVat != null && totalVat.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(createCreditNoteEntry(creditNote, vatAccount, totalVat, BigDecimal.ZERO, description));
        }
        entries.add(createCreditNoteEntry(creditNote, customerAccount, BigDecimal.ZERO, creditNote.getTotalAmount(), description));

        validateCreditNoteEntryTotals(entries);
        creditNoteEntryRepository.saveAll(entries);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteEntriesForCreditNoteCancellation(CreditNote creditNote) {
        creditNoteEntryRepository.deleteByCreditNote_IdNotaCredit(creditNote.getIdNotaCredit());
    }

    // --- MÉTODOS PRIVADOS DE AYUDA ---

    private Account findAccountBySettingKey(String key) {
        AccountingSetting setting = settingRepository.findById(key)
                .orElseThrow(() -> new BusinessRuleException("Configuración contable clave '" + key + "' no ha sido definida."));

        if (!setting.getAccount().isPostable()) {
            throw new BusinessRuleException("La cuenta '" + setting.getAccount().getAccountName() + "' configurada para '" + key + "' no es una cuenta de detalle (no es 'postable').");
        }
        return setting.getAccount();
    }

    private SaleEntry createSaleEntry(Sale sale, Account account, BigDecimal debe, BigDecimal haber, String description) {
        SaleEntry entry = new SaleEntry();
        entry.setSale(sale);
        entry.setAccount(account);
        entry.setDebe(debe); // <-- CAMBIO AQUÍ
        entry.setHaber(haber); // <-- CAMBIO AQUÍ
        entry.setDescription(description);
        return entry;
    }

    private void validateSaleEntryTotals(List<SaleEntry> entries) {
        BigDecimal totalDebits = entries.stream().map(SaleEntry::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add); // <-- CAMBIO AQUÍ
        BigDecimal totalCredits = entries.stream().map(SaleEntry::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add); // <-- CAMBIO AQUÍ

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalStateException("Asiento de Venta descuadrado. Debe: " + totalDebits + ", Haber: " + totalCredits);
        }
    }

    private CreditNoteEntry createCreditNoteEntry(CreditNote creditNote, Account account, BigDecimal debe, BigDecimal haber, String description) {
        CreditNoteEntry entry = new CreditNoteEntry();
        entry.setCreditNote(creditNote);
        entry.setAccount(account);
        entry.setDebe(debe); // <-- CAMBIO AQUÍ
        entry.setHaber(haber); // <-- CAMBIO AQUÍ
        entry.setDescription(description);
        return entry;
    }

    private void validateCreditNoteEntryTotals(List<CreditNoteEntry> entries) {
        BigDecimal totalDebits = entries.stream().map(CreditNoteEntry::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add); // <-- CAMBIO AQUÍ
        BigDecimal totalCredits = entries.stream().map(CreditNoteEntry::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add); // <-- CAMBIO AQUÍ

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalStateException("Asiento de Nota de Crédito descuadrado. Debe: " + totalDebits + ", Haber: " + totalCredits);
        }
    }
}