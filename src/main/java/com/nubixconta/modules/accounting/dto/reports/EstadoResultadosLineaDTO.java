package com.nubixconta.modules.accounting.dto.reports;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EstadoResultadosLineaDTO {
    private Integer idCatalog;
    private String accountCode;
    private String accountName;
    private BigDecimal totalPeriodo; // El saldo neto del período para esta cuenta
}