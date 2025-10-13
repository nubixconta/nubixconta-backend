package com.nubixconta.modules.purchases.dto.purchases;

import com.nubixconta.modules.accounting.dto.catalog.CatalogSummaryDTO;
import com.nubixconta.modules.inventory.dto.product.ProductSummaryDTO;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseDetailResponseDTO {
    private Integer purchaseDetailId;
    private ProductSummaryDTO product; // Será null si es un gasto
    private CatalogSummaryDTO catalog; // Será null si es un producto
    private String lineDescription;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private Boolean tax;
}