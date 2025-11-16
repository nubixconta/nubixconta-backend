package com.nubixconta.modules.accounting.dto.reports;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CategoriaBalanceDTO {
    private List<BalanceGeneralLineaDTO> cuentas;
    private BigDecimal subtotal;
}