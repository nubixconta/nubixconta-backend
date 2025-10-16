package com.nubixconta.modules.AccountsPayable.dto.AccountsPayable;

import com.nubixconta.modules.purchases.dto.purchases.PurchaseForAccountsPayableDTO;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountsPayablePurchaseResponseDTO {
    private BigDecimal balance;
    private BigDecimal payableAmount;
    private PurchaseForAccountsPayableDTO purchase;
}
