package com.nubixconta.modules.accounting.dto.reports;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BalanzaComprobacionLineaDTO {
    private Integer idCatalog;
    private String accountCode;
    private String accountName;

    // Saldos iniciales divididos
    private BigDecimal saldoInicialDeudor;  // Para Activos, Gastos
    private BigDecimal saldoInicialAcreedor; // Para Pasivos, Patrimonio, Ingresos

    // Movimientos del período
    private BigDecimal totalDebePeriodo;
    private BigDecimal totalHaberPeriodo;

    // Saldos finales divididos
    private BigDecimal saldoFinalDeudor;
    private BigDecimal saldoFinalAcreedor;
}