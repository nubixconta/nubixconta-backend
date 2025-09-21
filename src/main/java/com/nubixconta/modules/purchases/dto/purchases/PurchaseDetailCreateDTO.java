package com.nubixconta.modules.purchases.dto.purchases;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseDetailCreateDTO {

    // Una línea puede ser un producto o un gasto contable, pero no ambos.
    private Integer productId;
    private Integer catalogId; // <-- ID de la entrada en la tabla 'catalog'

    @Size(max = 255, message = "La descripción de la línea no puede exceder los 255 caracteres")
    private String lineDescription;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "Formato de precio unitario inválido")
    private BigDecimal unitPrice;

    @NotNull(message = "El subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "Formato de subtotal inválido")
    private BigDecimal subtotal;

    @NotNull(message = "Debe especificar si la línea lleva impuesto")
    private Boolean tax;
}