package com.nubixconta.modules.accounting.dto.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankEntryResponseDTO {
    private Integer idBankEntry;
    private Integer idCatalog;
    private String accountName; // <-- CAMPO AÑADIDO
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
    private LocalDateTime date;
    private Integer transactionBankId;
}
