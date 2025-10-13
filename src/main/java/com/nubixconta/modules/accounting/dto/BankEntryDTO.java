package com.nubixconta.modules.accounting.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankEntryDTO {
    private Integer idBankEntry;
    private Integer transactionBankId; // id de la transacción a la que pertenece
    private Integer idCatalog; // cuenta contable
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
    private LocalDateTime date;
}
