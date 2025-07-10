package com.nubixconta.modules.sales.dto.sales;

import com.nubixconta.modules.inventory.dto.product.ProductSummaryDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class SaleDetailResponseDTO {

    private Integer saleDetailId;

    private ProductSummaryDTO product; // null si es servicio

    private String serviceName;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}