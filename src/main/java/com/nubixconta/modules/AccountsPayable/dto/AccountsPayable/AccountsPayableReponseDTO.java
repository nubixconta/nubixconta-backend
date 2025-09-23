package com.nubixconta.modules.AccountsPayable.dto.AccountsPayable;

import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsResponseDTO;
import com.nubixconta.modules.purchases.dto.purchases.PurchaseDetailResponseDTO;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountsPayableReponseDTO {
    private BigDecimal balance;
    private BigDecimal payableAmount;
    private PurchaseDetailResponseDTO.PurchaseForAccountsPayableDTO purchase;
    private List<PaymentDetailsResponseDTO> paymentDetails;
}
