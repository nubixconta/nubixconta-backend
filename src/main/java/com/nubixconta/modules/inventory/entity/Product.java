package com.nubixconta.modules.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_product")
    private Integer idProduct;

    @NotBlank(message = "El código de producto es obligatorio")
    @Size(max = 10, message = "El código de producto puede tener máximo 10 caracteres")
    @Column(name = "product_code", length = 10, unique = true, nullable = false)
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

    @NotNull(message = "La fecha de producto es obligatoria")
    @Column(name = "product_date", nullable = false)
    private LocalDateTime productDate;

    @NotNull(message = "El estado del producto es obligatorio")
    @Column(name = "product_status", nullable = false)
    private Boolean productStatus;
}