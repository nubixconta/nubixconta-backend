package com.nubixconta.modules.accounting.dto.catalog;

import lombok.Data;

@Data
public class CatalogSummaryDTO {
    private Integer id; // ID de la entrada del catálogo (tabla 'catalog')
    private String accountCode; // Código de la cuenta (ej. "5.2.1")
    private String accountName; // Nombre de la cuenta (ej. "Gastos de Administración")
}