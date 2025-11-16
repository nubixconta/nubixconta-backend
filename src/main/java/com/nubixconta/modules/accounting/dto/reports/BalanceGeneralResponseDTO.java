package com.nubixconta.modules.accounting.dto.reports;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BalanceGeneralResponseDTO {
    // Secciones Detalladas y Jerárquicas
    private CategoriaBalanceDTO activoCorriente;
    private CategoriaBalanceDTO activoNoCorriente;
    private CategoriaBalanceDTO pasivoCorriente;
    private CategoriaBalanceDTO pasivoNoCorriente;
    private CategoriaBalanceDTO patrimonio;

    // Totales y Verificaciones
    private BigDecimal totalActivos;
    private BigDecimal totalPasivos;
    private BigDecimal totalPatrimonio;
    private BigDecimal totalPasivoYPatrimonio; // Nuevo para conveniencia del frontend
    private boolean balanceCuadrado;
}