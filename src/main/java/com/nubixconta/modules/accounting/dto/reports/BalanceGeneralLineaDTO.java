package com.nubixconta.modules.accounting.dto.reports;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BalanceGeneralLineaDTO {
    private Integer idCatalog;
    private String accountCode;
    private String accountName;
    private BigDecimal saldoFinal; // El saldo final de la cuenta a la fecha de corte
}