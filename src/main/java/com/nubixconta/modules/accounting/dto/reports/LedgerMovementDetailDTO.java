package com.nubixconta.modules.accounting.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor // JPQL necesita este constructor para crear el objeto
public class LedgerMovementDetailDTO {
    // Campos de GeneralLedgerView
    private String uniqueId;
    private Long documentId;
    private String documentType;
    private LocalDateTime accountingDate;
    private Integer idCatalog;

    // Campos extra de las tablas unidas
    private String accountCode;
    private String accountName;

    // Campos de GeneralLedgerView (continuación)
    private BigDecimal debe;
    private BigDecimal haber;
    private String description;
    private Integer companyId;
}