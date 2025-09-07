package com.nubixconta.modules.accountsreceivable.dto.accountsreceivable;

import com.nubixconta.modules.sales.dto.sales.SaleForAccountsReceivableDTO;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class AccountsReceivableSaleResponseDTO {
    private BigDecimal balance;
    private SaleForAccountsReceivableDTO sale;
}
