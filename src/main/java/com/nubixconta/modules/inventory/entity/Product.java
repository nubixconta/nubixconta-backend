package com.nubixconta.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // Primary Key autoincremental

    @Column(name = "product_code", length = 10, unique = true, nullable = false)
    private String productCode;

    @Column(name = "product_name", length = 50, nullable = false)
    private String productName;

    @Column(name = "unit", length = 20, nullable = false)
    private String unit;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "product_date")
    private LocalDateTime productDate;

    @Column(name = "product_status")
    private Boolean productStatus;
}