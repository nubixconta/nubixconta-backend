package com.nubixconta.modules.sales.dto.creditnote;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
public class CreditNoteDetailResponseDTO {
    private Integer creditNoteDetailId;
    private Integer productId;
    private String productName;
    private String productCode;
    private String serviceName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}