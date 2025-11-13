package com.nubixconta.modules.accounting.dto.CollectionEntry;

import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailFromEntryResponseDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CollectionEntryFronBankResponseDTO {
    private Integer id;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
    private LocalDateTime date;
    private CollectionDetailFromEntryResponseDTO collection;
}
