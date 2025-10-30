package com.nubixconta.modules.accounting.dto.accounting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryLineResponseDTO { // <--- NOMBRE CAMBIADO
    private Long id;
    private Integer catalogId;
    private String accountCode;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
}