package com.nubixconta.modules.sales.dto.sales;

import com.nubixconta.modules.sales.dto.customer.CustomerSummaryDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SaleResponseDTO {

    private Integer saleId;
    private CustomerSummaryDTO customer;
    private String documentNumber;
    private String saleStatus;
    private LocalDateTime issueDate;
    private String saleType;
    private BigDecimal totalAmount;
    // --- ¡AÑADIR ESTOS DOS CAMPOS NUEVOS! ---
    private BigDecimal subtotalAmount;
    private BigDecimal vatAmount;

    private String saleDescription;
    private String moduleType;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;

    private List<SaleDetailResponseDTO> saleDetails;

}