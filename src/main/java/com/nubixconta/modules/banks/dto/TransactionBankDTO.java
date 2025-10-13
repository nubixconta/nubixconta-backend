package com.nubixconta.modules.banks.dto;

import com.nubixconta.modules.accounting.dto.BankEntryDTO;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionBankDTO {
    private Integer idBankTransaction;
    private LocalDateTime transactionDate;
    private String transactionType;
    private String receiptNumber;
    private String description;
    private String accountingTransactionStatus;
    private String moduleType;
    private Integer companyId;

    // Lista de movimientos (detalles contables)
    private List<BankEntryDTO> bankEntries;
}
