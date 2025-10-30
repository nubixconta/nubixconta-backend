package com.nubixconta.modules.accounting.entity;

import com.nubixconta.modules.accounting.entity.enums.AccountingTransactionStatus;
import com.nubixconta.modules.administration.entity.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "transaction_accounting")
@Getter
@Setter
@NoArgsConstructor
public class TransactionAccounting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_transaction_id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "accounting_transaction_status", length = 20, nullable = false)
    private AccountingTransactionStatus status;

    @Column(name = "module_type", length = 30, nullable = false)
    private String moduleType;

    @Column(name = "total_debe", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalDebe;

    @Column(name = "total_haber", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalHaber;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    // Relación Uno a Muchos con las líneas del asiento.
    // Cascade.ALL y orphanRemoval=true aseguran que las líneas se gestionen junto con la cabecera.
    @OneToMany(mappedBy = "transactionAccounting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AccountingEntry> accountingEntries = new HashSet<>();

    // --- Métodos de ayuda para gestionar la relación bidireccional ---

    public void addEntry(AccountingEntry entry) {
        accountingEntries.add(entry);
        entry.setTransactionAccounting(this);
    }

    public void removeEntry(AccountingEntry entry) {
        accountingEntries.remove(entry);
        entry.setTransactionAccounting(null);
    }

    // --- Buenas prácticas para JPA ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionAccounting that = (TransactionAccounting) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}