package com.nubixconta.modules.accounting.dto.PaymentEntry;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentEntryResponseDTO {
    private Integer id;
    private String codAccount;
    private String documentNumber;
    private String supplierName;
    private String tipo;
    private String status;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
    private LocalDateTime date;
}
