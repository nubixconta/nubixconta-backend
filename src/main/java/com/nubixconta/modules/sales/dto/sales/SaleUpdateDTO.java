package com.nubixconta.modules.sales.dto.sales;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SaleUpdateDTO {
    private String documentNumber;
    private String saleStatus;
    private LocalDateTime issueDate;
    private String saleType;
    private BigDecimal totalAmount;
    private String saleDescription;
    private List<SaleDetailCreateDTO> saleDetails;

}