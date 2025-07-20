package com.nubixconta.modules.inventory.dto.product;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ProductResponseDTO {

    private Integer idProduct;

    private String productCode;

    private String productName;

    private String unit;

    private Integer stockQuantity;

    private Boolean productStatus;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;
}
