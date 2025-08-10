package com.nubixconta.modules.inventory.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.nubixconta.modules.administration.entity.Company;
import org.hibernate.annotations.*;
import java.time.LocalDateTime;

@Table(name = "product", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"company_id", "product_code"})
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_product")
    private Integer idProduct;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank(message = "El código de producto es obligatorio")
    @Size(max = 10, message = "El código de producto puede tener máximo 10 caracteres")
    @Column(name = "product_code", length = 10, nullable = false)
    private String productCode;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 50, message = "El nombre puede tener máximo 50 caracteres")
    @Column(name = "product_name", length = 50, nullable = false)
    private String productName;

    @NotBlank(message = "La unidad es obligatoria")
    @Size(max = 20, message = "La unidad puede tener máximo 20 caracteres")
    @Column(name = "unit", length = 20, nullable = false)
    private String unit;

    @NotNull(message = "La cantidad en stock es obligatoria")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @NotNull(message = "El estado del producto es obligatorio")
    @Column(name = "product_status", nullable = false)
    private Boolean productStatus;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product that)) return false;
        // La igualdad para una entidad con ID debe basarse en el ID.
        // Si idProduct es nulo, dos entidades no pueden ser iguales a menos que sean la misma instancia.
        return idProduct != null && idProduct.equals(that.idProduct);
    }

    @Override
    public int hashCode() {
        // Usar una constante (el hash de la clase) es la práctica estándar para entidades JPA.
        // Esto evita que el hash cambie cuando se asigna un ID.
        return getClass().hashCode();
    }
}