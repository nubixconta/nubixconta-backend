package com.nubixconta.modules.banks.dto;

import com.nubixconta.modules.accounting.dto.BankEntryDTO;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal; 


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionBankDTO {
    private Integer idBankTransaction;
    private LocalDate transactionDate;
    private BigDecimal totalAmount;
    private String transactionType;
    private String receiptNumber;
    private String description;
    private String accountingTransactionStatus;
    private String moduleType;
    private Integer companyId;

    // Lista de movimientos (detalles contables)
    private List<BankEntryDTO> bankEntries;
}
