package com.nubixconta.modules.purchases.dto.creditnote;

import com.nubixconta.modules.accounting.dto.catalog.CatalogSummaryDTO;
import com.nubixconta.modules.inventory.dto.product.ProductSummaryDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseCreditNoteDetailResponseDTO {
    private Integer id;
    private ProductSummaryDTO product; // Null si es gasto
    private CatalogSummaryDTO catalog; // Null si es producto
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}