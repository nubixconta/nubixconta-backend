package com.nubixconta.modules.accounting.dto.reports;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class EstadoResultadosResponseDTO {
    // Secciones Detalladas (más granulares)
    private List<EstadoResultadosLineaDTO> ingresosOperacionales;
    private List<EstadoResultadosLineaDTO> costoVenta; // Renombrado para claridad
    private List<EstadoResultadosLineaDTO> gastosVenta;
    private List<EstadoResultadosLineaDTO> gastosAdministracion;
    private List<EstadoResultadosLineaDTO> otrosIngresos; // Ingresos no operacionales
    private List<EstadoResultadosLineaDTO> otrosGastos;   // Gastos financieros, etc.

    // Totales y Subtotales Calculados (más detallados)
    private BigDecimal totalIngresosOperacionales;
    private BigDecimal totalCostoVenta;
    private BigDecimal utilidadBruta; // Ingresos Op. - Costo Venta

    private BigDecimal totalGastosVenta;
    private BigDecimal totalGastosAdministracion;
    private BigDecimal totalGastosOperacionales; // Gasto Venta + Gasto Admin
    private BigDecimal utilidadOperacional; // Utilidad Bruta - Gastos Op.

    private BigDecimal totalOtrosIngresos;
    private BigDecimal totalOtrosGastos;
    private BigDecimal utilidadAntesImpuestos; // Utilidad Op. + Otros Ingresos - Otros Gastos

    private BigDecimal reservaLegal; // Cálculo (ej. 7%)
    private BigDecimal impuestoSobreLaRenta; // Cálculo (ej. 30%)
    private BigDecimal utilidadDelEjercicio; // El resultado final
}