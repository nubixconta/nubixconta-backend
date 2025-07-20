package com.nubixconta.modules.sales.dto.sales;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SaleForAccountsReceivableDTO {
    private String documentNumber;
    private BigDecimal totalAmount;
    private LocalDateTime issueDate;
    // Campos de cliente
    private String customerName;
    private String customerLastName;
    private Integer creditDay;
}