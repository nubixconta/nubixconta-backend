package com.nubixconta.modules.purchases.dto.purchases;

import com.nubixconta.modules.purchases.dto.supplier.SupplierSummaryDTO;

import java.time.LocalDateTime;

public class PurchaseSummaryDTO {
    private Integer idPurchase;
    private String documentNumber;
    private LocalDateTime issueDate;
    private String purchaseDescription;
    private SupplierSummaryDTO customer;
}
