package com.nubixconta.modules.purchases.dto.incometax;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IncomeTaxCreateDTO {

    @NotNull(message = "El ID de la compra asociada es obligatorio")
    private Integer purchaseId;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 100, message = "El número de documento no puede exceder los 100 caracteres")
    private String documentNumber;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 256, message = "La descripción no puede exceder los 256 caracteres")
    private String description;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDateTime issueDate;

    @NotNull(message = "El monto a aplicar es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "Formato de monto inválido")
    @Positive(message = "El monto debe ser mayor que cero")
    private BigDecimal amountIncomeTax;
}