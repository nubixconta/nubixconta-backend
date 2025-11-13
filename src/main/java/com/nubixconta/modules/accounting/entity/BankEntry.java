package com.nubixconta.modules.accounting.entity;

import com.nubixconta.modules.banks.entity.TransactionBank;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_entry")
@Getter
@Setter
@NoArgsConstructor
public class BankEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bank_entry")
    private Integer idBankEntry;

    // Relación hacia el módulo de Bancos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bank_transaction", nullable = false)
    private TransactionBank transactionBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_catalog", nullable = false)
    private Catalog idCatalog;

    @Digits(integer = 10, fraction = 2, message = "El valor del débito debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "debit", precision = 10, scale = 2)
    private BigDecimal debit;

    @Digits(integer = 10, fraction = 2, message = "El valor del crédito debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "credit", precision = 10, scale = 2)
    private BigDecimal credit;

    @NotBlank(message = "La descripción del movimiento es obligatoria")
    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @CreationTimestamp
    @Column(name = "date", nullable = false)
    private LocalDateTime date;
}
