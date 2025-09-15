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


    // El asiento contable ahora apunta a la 'activación' específica (Catalog)
    // en lugar de a la cuenta maestra (Account). Esto asegura la integridad.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_catalog", nullable = false)
    private Catalog catalog;


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