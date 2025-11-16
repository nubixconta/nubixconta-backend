package com.nubixconta.modules.accounting.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CierreMensualStatusDTO {
    private int mes;
    private String nombreMes;
    private boolean cerrado;
}