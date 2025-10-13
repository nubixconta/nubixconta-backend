package com.nubixconta.modules.purchases.dto.purchases;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
public class PurchaseForAccountsPayableDTO {
    //Campos de la compra
    private String documentNumber;
    private Integer idPurchase;
    private BigDecimal totalAmount;
    private LocalDateTime issueDate;
    private String purchaseDescription;
    // Campos de proveedor
    private String supplierName;
    private String supplierLastName;
    private Integer creditDay;
}
