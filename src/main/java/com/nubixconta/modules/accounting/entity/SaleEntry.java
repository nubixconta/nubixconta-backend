package com.nubixconta.modules.accounting.entity;

import com.nubixconta.modules.sales.entity.Sale;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_entry")
@Getter
@Setter
@NoArgsConstructor
public class SaleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_entry_id")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "debe", precision = 12, scale = 2, nullable = false)
    private BigDecimal debe = BigDecimal.ZERO;

    // --- ¡CAMBIO AQUÍ! ---
    @Column(name = "haber", precision = 12, scale = 2, nullable = false)
    private BigDecimal haber = BigDecimal.ZERO;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "date", nullable = false, updatable = false)
    private LocalDateTime date;
}