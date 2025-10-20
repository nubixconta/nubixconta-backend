package com.nubixconta.modules.banks.entity;

import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.accounting.entity.BankEntry;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "transaction_bank")
@Getter
@Setter
@NoArgsConstructor
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public class TransactionBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bank_transaction")
    private Integer idBankTransaction;

    @NotNull(message = "La fecha de la transacción es obligatoria")
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @NotNull
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @NotBlank(message = "El tipo de transacción es obligatorio")
    @Size(max = 30, message = "El tipo de transacción puede tener máximo 30 caracteres")
    @Column(name = "transaction_type", length = 30, nullable = false)
    private String transactionType;

    @NotBlank(message = "El número de comprobante es obligatorio")
    @Size(max = 15, message = "El número de comprobante puede tener máximo 15 caracteres")
    @Column(name = "receipt_number", length = 15, nullable = false)
    private String receiptNumber;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción puede tener máximo 255 caracteres")
    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @NotBlank(message = "El estado contable de la transacción es obligatorio")
    @Size(max = 20, message = "El estado contable puede tener máximo 20 caracteres")
    @Column(name = "accounting_transaction_status", length = 20, nullable = false)
    private String accountingTransactionStatus;

    @NotBlank(message = "El tipo de módulo es obligatorio")
    @Size(max = 100, message = "El tipo de módulo puede tener máximo 100 caracteres")
    @Column(name = "module_type", length = 100, nullable = false)
    private String moduleType;

    @NotNull(message = "La empresa es obligatoria")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Relación con movimientos contables (desde el módulo de contabilidad)
    // mappedBy debe coincidir con el nombre del atributo en BankEntry
    @OneToMany(mappedBy = "transactionBank", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BankEntry> bankEntries = new HashSet<>();

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    public void addBankEntry(BankEntry entry) {
        if (bankEntries == null) bankEntries = new HashSet<>();
        bankEntries.add(entry);
        entry.setTransactionBank(this);
    }

    public void removeBankEntry(BankEntry entry) {
        if (bankEntries != null) {
            bankEntries.remove(entry);
            entry.setTransactionBank(null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionBank that)) return false;
        return idBankTransaction != null && idBankTransaction.equals(that.idBankTransaction);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
