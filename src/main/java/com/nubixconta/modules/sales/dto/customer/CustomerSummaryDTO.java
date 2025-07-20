package com.nubixconta.modules.sales.dto.customer;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class CustomerSummaryDTO {
    private Integer clientId;
    private String customerName;
    private String customerLastName;
    private String customerDui;
    private String customerNit;
    private Integer creditDay;
}