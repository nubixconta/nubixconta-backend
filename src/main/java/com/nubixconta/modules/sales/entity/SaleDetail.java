package com.nubixconta.modules.sales.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.nubixconta.modules.inventory.entity.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "sale_detail")
@Getter
@Setter
@NoArgsConstructor
public class SaleDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_detail_id")
    private Integer saleDetailId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    @JsonIgnoreProperties({
            "customer",
            "saleStatus",
            "issueDate",
            "saleType",
            "totalAmount",
            "saleDate",
            "moduleType",
            "saleDetails"
    })
    private Sale sale;


    @ManyToOne
    @JoinColumn(name = "id_product", referencedColumnName = "id_product", nullable = true)
    @JsonIgnoreProperties({
            "unit",
            "stockQuantity",
            "productDate",
            "productStatus"
    })
    private Product product;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;


    @Size(max = 50, message = "El nombre del servicio puede tener máximo 50 caracteres")
    @Column(name = "service_name", length = 50)
    private String serviceName;

    @NotNull(message = "El precio unitario es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El precio unitario debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @NotNull(message = "El subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El subtotal debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaleDetail that = (SaleDetail) o;
        // La igualdad completa debe considerar al padre
        return Objects.equals(sale, that.sale) &&
                Objects.equals(product, that.product) &&
                Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sale, product, serviceName);
    }
}
