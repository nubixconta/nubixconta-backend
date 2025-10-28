package com.nubixconta.modules.accounting.dto.catalog;

import lombok.Data;

@Data
public class UpdateCatalogDTO {
    // Se envían como null si el usuario quiere revertir al valor por defecto
    private String customName;
    private String customCode;
}