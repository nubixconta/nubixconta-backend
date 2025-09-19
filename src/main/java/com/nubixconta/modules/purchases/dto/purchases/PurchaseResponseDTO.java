package com.nubixconta.modules.purchases.dto.purchases;

import com.nubixconta.modules.purchases.dto.supplier.SupplierSummaryDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseResponseDTO {
    private Integer idPurchase;
    private SupplierSummaryDTO supplier;
    private String documentNumber;
    private String purchaseStatus;
    private LocalDateTime issueDate;
    private BigDecimal subtotalAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private String purchaseDescription;
    private String moduleType;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
    private List<PurchaseDetailResponseDTO> purchaseDetails;
}