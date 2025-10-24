package com.nubixconta.modules.purchases.dto.incometax;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IncomeTaxUpdateDTO {

    @Size(max = 100, message = "El número de documento no puede exceder los 100 caracteres")
    private String documentNumber;

    @Size(max = 256, message = "La descripción no puede exceder los 256 caracteres")
    private String description;

    private LocalDateTime issueDate;

    @Digits(integer = 10, fraction = 2, message = "Formato de monto inválido")
    @Positive(message = "El monto debe ser mayor que cero")
    private BigDecimal amountIncomeTax;
}