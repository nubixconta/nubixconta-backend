package com.nubixconta.modules.purchases.dto.purchases;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseForCreditNoteDTO {
    private Integer idPurchase;
    private String documentNumber;
    private LocalDateTime issueDate;
    private BigDecimal totalAmount;
}