package com.nubixconta.modules.accounting.entity;

import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_entry")
@Data
public class CollectionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collection_entry_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "collection_detail_id", nullable = false)
    private CollectionDetail collectionDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", nullable = false)
    private Catalog catalog;

    @Digits(integer = 8, fraction = 2, message = "El debito no puede tener hasta 8 dígitos enteros y 2 decimales")
    @DecimalMin(value="0.00",inclusive=true,message = "El debito no puede ser negativo")
    @NotNull(message = "El debe es obligatorio")
    @Column( precision = 10, scale = 2)
    private BigDecimal debit;

    @Digits(integer = 8, fraction = 2, message = "El credito no puede tener hasta 8 dígitos enteros y 2 decimales")
    @DecimalMin(value="0.00",inclusive=true,message = "El credito no puede ser negativo")
    @NotNull(message = "El haber es obligatorio")
    @Column( precision = 10, scale = 2)
    private BigDecimal credit;

    @NotNull(message = "La descripcion es obligatorio")
    @Size(max = 50, message = "La descripción no puede tener más 50 caracteres")
    @Column(length = 50)
    private String description;

    @NotNull(message = "La fecha es obligatorio")
    private LocalDateTime date;

}
