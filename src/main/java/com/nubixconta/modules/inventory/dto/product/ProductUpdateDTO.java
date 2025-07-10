package com.nubixconta.modules.inventory.dto.product;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ProductUpdateDTO {
    private String productCode;

    private String productName;

    private String unit;

    private Boolean productStatus;
}
