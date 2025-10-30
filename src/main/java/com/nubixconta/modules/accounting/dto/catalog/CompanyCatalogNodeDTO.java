package com.nubixconta.modules.accounting.dto.catalog;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class CompanyCatalogNodeDTO {
    private Integer id; // ID de la tabla 'catalog'
    private Integer masterAccountId; // ID de la cuenta maestra original (tabla 'account')
    private String effectiveName;
    private String effectiveCode;
    private boolean isPostable;
    private boolean isActive;
    private List<CompanyCatalogNodeDTO> children = new ArrayList<>();
}