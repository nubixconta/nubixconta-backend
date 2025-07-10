package com.nubixconta.modules.inventory.dto.product;


import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ProductSummaryDTO {
    private Integer idProduct;
    private String productCode;
    private String productName;
}