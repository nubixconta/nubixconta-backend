package com.nubixconta.modules.accounting.entity;

import com.nubixconta.modules.purchases.entity.IncomeTax;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una línea individual (un movimiento de debe o haber) dentro de la
 * partida contable generada por una Retención de Impuesto Sobre la Renta (ISR).
 */
@Entity
@Table(name = "income_tax_entry")
@Getter
@Setter
@NoArgsConstructor
public class IncomeTaxEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "income_tax_entry_id")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_income_tax", nullable = false)
    private IncomeTax incomeTax;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_catalog", nullable = false)
    private Catalog catalog;

    @Column(name = "debe", precision = 12, scale = 2, nullable = false)
    private BigDecimal debe = BigDecimal.ZERO;

    @Column(name = "haber", precision = 12, scale = 2, nullable = false)
    private BigDecimal haber = BigDecimal.ZERO;

    @Column(name = "description", length = 256)
    private String description;

    @CreationTimestamp
    @Column(name = "date", nullable = false, updatable = false)
    private LocalDateTime date;
}