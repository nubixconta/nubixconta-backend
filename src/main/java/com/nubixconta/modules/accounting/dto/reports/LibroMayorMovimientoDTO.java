package com.nubixconta.modules.accounting.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor // Constructor para facilitar la creación
public class LibroMayorMovimientoDTO {
    private LocalDateTime accountingDate;
    private String documentType;
    private Long documentId;
    private String description;
    private BigDecimal debe;
    private BigDecimal haber;
}