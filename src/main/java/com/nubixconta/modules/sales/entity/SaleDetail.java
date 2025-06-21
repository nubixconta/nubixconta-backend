package com.nubixconta.modules.sales.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nubixconta.modules.inventory.entity.Product;
import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_detail")
@Data
public class SaleDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_detail_id")
    private Integer saleDetailId;

    // Relación con Sale (evita recursividad en JSON)
    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false)
    @JsonIgnore
    private Sale sale;

    // Relación con Product (clave foránea por código)
    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id_product", nullable = false)
    private Product product;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "service_name", length = 50)
    private String serviceName;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;
}