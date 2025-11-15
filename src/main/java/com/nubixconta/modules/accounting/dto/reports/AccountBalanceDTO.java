package com.nubixconta.modules.accounting.dto.reports;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class AccountBalanceDTO {
    private Integer idCatalog;
    private BigDecimal totalDebe;
    private BigDecimal totalHaber;
    private BigDecimal saldo; // Usado para el saldo inicial

    // Constructor para Movimientos del Período
    public AccountBalanceDTO(Integer idCatalog, BigDecimal totalDebe, BigDecimal totalHaber) {
        this.idCatalog = idCatalog;
        this.totalDebe = totalDebe != null ? totalDebe : BigDecimal.ZERO;
        this.totalHaber = totalHaber != null ? totalHaber : BigDecimal.ZERO;
        this.saldo = null;
    }

    // Constructor para Saldos Iniciales
    public AccountBalanceDTO(Integer idCatalog, BigDecimal saldo) {
        this.idCatalog = idCatalog;
        this.saldo = saldo != null ? saldo : BigDecimal.ZERO;
        this.totalDebe = null;
        this.totalHaber = null;
    }
}