package com.nubixconta.modules.purchases.entity;

import com.nubixconta.modules.accounting.entity.Catalog; // CORRECCIÓN: Import correcto.
import com.nubixconta.modules.inventory.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "purchase_credit_note_detail")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseCreditNoteDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_note_detail_id")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_credit_note", nullable = false)
    private PurchaseCreditNote purchaseCreditNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product", referencedColumnName = "id_product")
    private Product product;

    // CORRECCIÓN: La relación ya no es con 'ChartOfAccounts', sino con 'Catalog'.
    // Esto es idéntico a como está modelado en 'PurchaseDetail'.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_catalog", nullable = true)
    private Catalog catalog;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @NotNull(message = "El subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseCreditNoteDetail that = (PurchaseCreditNoteDetail) o;

        // CORRECCIÓN: La igualdad se define por el padre y la clave de negocio (producto o catálogo).
        return Objects.equals(purchaseCreditNote, that.purchaseCreditNote) &&
                Objects.equals(product, that.product) &&
                Objects.equals(catalog, that.catalog);
    }

    @Override
    public int hashCode() {
        // CORRECCIÓN: El hash code se basa en los mismos campos.
        return Objects.hash(purchaseCreditNote, product, catalog);
    }
}