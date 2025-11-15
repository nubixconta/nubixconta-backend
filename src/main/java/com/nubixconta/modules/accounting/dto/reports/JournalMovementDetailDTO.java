package com.nubixconta.modules.accounting.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de Proyección para la consulta del Libro Diario.
 * Contiene todos los campos necesarios de la vista y las tablas unidas.
 */
@Data
@AllArgsConstructor // JPQL necesita este constructor
public class JournalMovementDetailDTO {
    private Long documentId;
    private String documentType;
    private LocalDateTime accountingDate;
    private Integer idCatalog;
    private String accountCode; // Campo extra de la cuenta
    private String accountName; // Campo extra de la cuenta
    private BigDecimal debe;
    private BigDecimal haber;
    private String description;
}