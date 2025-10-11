package com.nubixconta.modules.purchases.entity;

import com.nubixconta.modules.accounting.entity.Catalog; // <-- NUEVO IMPORT
import com.nubixconta.modules.inventory.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "purchasedetail")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_detail_id")
    private Integer purchaseDetailId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_purchase", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product", nullable = true)
    private Product product;

    // --- CAMBIO CLAVE #1: Se añade la relación con el Catálogo Contable ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_catalog", nullable = true)
    private Catalog catalog;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // --- CAMBIO CLAVE #2: 'serviceName' se reemplaza por 'lineDescription' ---
    @Size(max = 255)
    @Column(name = "line_description", length = 255)
    private String lineDescription;

    @NotNull
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @NotNull
    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @NotNull
    @Column(name = "tax", nullable = false)
    private Boolean tax;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseDetail that = (PurchaseDetail) o;
        if (purchaseDetailId != null && that.purchaseDetailId != null) {
            return purchaseDetailId.equals(that.purchaseDetailId);
        }
        return Objects.equals(purchase, that.purchase) &&
                Objects.equals(product, that.product) &&
                Objects.equals(catalog, that.catalog) && // <-- Añadido para consistencia
                Objects.equals(lineDescription, that.lineDescription); // <-- Añadido para consistencia
    }

    @Override
    public int hashCode() {
        return purchaseDetailId != null ? purchaseDetailId.hashCode() : Objects.hash(purchase, product, catalog, lineDescription);
    }
}