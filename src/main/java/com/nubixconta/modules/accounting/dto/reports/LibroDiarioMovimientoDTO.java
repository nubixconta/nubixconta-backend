package com.nubixconta.modules.accounting.dto.reports;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LibroDiarioMovimientoDTO {
    // Nota: Omitimos uniqueId y companyId, ya que el frontend no los necesita.

    private Long documentId;
    private String documentType;
    private LocalDateTime accountingDate;
    private Integer idCatalog;
    private String accountCode;     // ¡NUEVO! El código efectivo de la cuenta (ej. "1101-01")
    private String accountName;     // ¡NUEVO! El nombre efectivo de la cuenta (ej. "Caja")
    private BigDecimal debe;
    private BigDecimal haber;
    private String description;
}