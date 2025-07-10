package com.nubixconta.modules.sales.dto.sales;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class SaleDetailCreateDTO {

    // Producto o servicio, solo uno será no-nulo
    private Integer productId;

    @Size(max = 50, message = "El nombre del servicio puede tener máximo 50 caracteres")
    private String serviceName;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "Debe tener hasta 10 dígitos y 2 decimales")
    private BigDecimal unitPrice;

    @NotNull(message = "El subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "Debe tener hasta 10 dígitos y 2 decimales")
    private BigDecimal subtotal;

    // Si productId != null entonces serviceName debe ser null, y viceversa
}