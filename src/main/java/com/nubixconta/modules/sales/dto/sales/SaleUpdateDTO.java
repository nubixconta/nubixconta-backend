package com.nubixconta.modules.sales.dto.sales;
import java.util.List;

import jakarta.validation.Valid;
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
    private LocalDateTime issueDate;
    private String saleType;
    private BigDecimal totalAmount;
    private String saleDescription;
    private List<@Valid SaleDetailCreateDTO> saleDetails;

}