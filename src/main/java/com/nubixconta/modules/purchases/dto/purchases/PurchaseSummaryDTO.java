package com.nubixconta.modules.purchases.dto.purchases;

import com.nubixconta.modules.purchases.dto.supplier.SupplierSummaryDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PurchaseSummaryDTO {
    private Integer idPurchase;
    private String documentNumber;
    private LocalDateTime issueDate;
    private String purchaseDescription;
    private SupplierSummaryDTO supplier;
}
