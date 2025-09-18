package com.nubixconta.modules.purchases.entity;

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
    @JoinColumn(name = "id_product", nullable = true) // Nullable porque puede ser un servicio
    private Product product;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Size(max = 50)
    @Column(name = "service_name", length = 50) // Nullable porque puede ser un producto
    private String serviceName;

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
        // La igualdad se basa en el ID si la entidad ya está persistida
        if (purchaseDetailId != null && that.purchaseDetailId != null) {
            return purchaseDetailId.equals(that.purchaseDetailId);
        }
        // Para entidades nuevas, se compara el contenido
        return Objects.equals(purchase, that.purchase) &&
                Objects.equals(product, that.product) &&
                Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        // Usar una constante para entidades nuevas (sin ID)
        // y el hash del ID para las persistidas.
        return purchaseDetailId != null ? purchaseDetailId.hashCode() : Objects.hash(purchase, product, serviceName);
    }
}