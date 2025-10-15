package com.nubixconta.modules.purchases.dto.creditnote;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseCreditNoteDetailCreateDTO {

    // Un detalle debe ser un producto o un gasto, pero no ambos.
    private Integer productId;
    private Integer catalogId;

    @NotNull(message = "La cantidad a devolver es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal unitPrice;

    @NotNull(message = "El subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal subtotal;

    @NotNull(message = "Debe especificar si el detalle lleva impuesto.")
    private Boolean tax;

    private String lineDescription;
}