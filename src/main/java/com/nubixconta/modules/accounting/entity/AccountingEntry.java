package com.nubixconta.modules.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_entry")
@Getter
@Setter
@NoArgsConstructor
public class AccountingEntry {

    // Aunque el diagrama sugiere una clave compuesta, usar una clave subrogada simple (un ID único)
    // es una práctica recomendada en JPA para simplificar la gestión de la entidad.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accounting_entry_id")
    private Long id;

    // Relación Muchos a Uno con la cabecera del asiento. Es la propietaria de la relación.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_transaction_id", nullable = false)
    private TransactionAccounting transactionAccounting;

    // Relación clave con el catálogo de cuentas de la empresa.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_catalog", nullable = false)
    private Catalog catalog;

    @Column(name = "debe", precision = 10, scale = 2, nullable = false)
    private BigDecimal debe;

    @Column(name = "haber", precision = 10, scale = 2, nullable = false)
    private BigDecimal haber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    // --- Buenas prácticas para JPA ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountingEntry that = (AccountingEntry) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}