package com.nubixconta.modules.purchases.dto.incometax;

import com.nubixconta.modules.purchases.dto.purchases.PurchaseSummaryDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IncomeTaxResponseDTO {

    private Integer idIncomeTax;
    private String documentNumber;
    private String incomeTaxStatus;
    private String description;
    private LocalDateTime issueDate;
    private BigDecimal amountIncomeTax;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
    private PurchaseSummaryDTO purchase; // Objeto anidado con info de la compra
}