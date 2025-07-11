package com.nubixconta.modules.sales.dto.creditnote;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
public class CreditNoteDetailCreateDTO {
    private Integer creditNoteDetailId;
    private Integer productId; // Puede ser null si es servicio

    @Size(max = 50, message = "El nombre del servicio puede tener máximo 50 caracteres")
    private String serviceName; // Puede ser null si es producto

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    private BigDecimal unitPrice;

    @NotNull(message = "El subtotal es obligatorio")
    private BigDecimal subtotal;
}