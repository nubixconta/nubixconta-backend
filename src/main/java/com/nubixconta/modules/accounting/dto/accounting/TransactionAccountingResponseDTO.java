package com.nubixconta.modules.accounting.dto.accounting;

import com.nubixconta.modules.accounting.entity.enums.AccountingTransactionStatus;
// ASEGÚRATE DE QUE ESTE IMPORT APUNTE AL NUEVO DTO
import com.nubixconta.modules.accounting.dto.accounting.JournalEntryLineResponseDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class TransactionAccountingResponseDTO {
    private Long id;
    private LocalDateTime transactionDate;
    private String description;
    private AccountingTransactionStatus status;
    private String moduleType;
    private BigDecimal totalDebe;
    private BigDecimal totalHaber;
    private LocalDateTime creationDate;

    // AQUÍ ESTÁ EL CAMBIO PRINCIPAL: USA EL NUEVO DTO
    private Set<JournalEntryLineResponseDTO> entries;
}