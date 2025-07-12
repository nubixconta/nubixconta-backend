package com.nubixconta.modules.inventory.dto.movement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor // Útil para la construcción
public class MovementProductInfoDTO {
    private Integer productId;
    private String productCode;
    private String productName;
}