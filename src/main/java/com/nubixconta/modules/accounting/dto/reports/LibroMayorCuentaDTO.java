package com.nubixconta.modules.accounting.dto.reports;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LibroMayorCuentaDTO {
    private Integer idCatalog;
    private String accountCode;
    private String accountName;

    // --- Totales ---
    private BigDecimal totalDebe;
    private BigDecimal totalHaber;
    private BigDecimal saldoPeriodo; // Resultado (Total Debe - Total Haber)

    // --- Detalle ---
    private List<LibroMayorMovimientoDTO> movimientos;
}