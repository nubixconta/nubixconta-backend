package com.nubixconta.modules.purchases.dto.purchases;

import com.nubixconta.modules.purchases.dto.supplier.SupplierSummaryDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseSummaryDTO {
    private Integer idPurchase;
    private String documentNumber;
    private LocalDateTime issueDate;
    private String purchaseDescription;
    private SupplierSummaryDTO supplier;
    private BigDecimal subtotalAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
}
