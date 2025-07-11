package com.nubixconta.modules.sales.dto.sales;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
public class SaleSummaryDTO {
    private Integer saleId;
    private String documentNumber;
    private LocalDateTime issueDate;
    private String saleDescription;
    // Si es necesario agregar más campos
}