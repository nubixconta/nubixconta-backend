package com.nubixconta.modules.AccountsPayable.dto.PaymentDetails;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDetailsCreateDTO {
    @NotNull(message = "El ID de la venta es obligatorio")
    private Integer idPurchase;

    @NotNull(message = "La referencia es obligatoria")
    @Size(max = 30, message = "La referencia no puede tener más de 30 caracteres")
    private String reference;

    @NotNull(message = "El método de pago es obligatorio")
    @Size(max = 20, message = "El método no puede tener más de 20 caracteres")
    private String paymentMethod;

    @NotNull(message = "El estado del pago es obligatorio")
    @Size(max = 10, message = "El estado no puede tener más de 10 caracteres")
    private String paymentStatus;

    @NotNull(message = "El monto es obligatorio")
    @Digits(integer = 8, fraction = 2, message = "El monto puede tener hasta 8 dígitos enteros y 2 decimales")
    @DecimalMin(value = "0.00", inclusive = true, message = "El monto no puede ser negativo")
    private BigDecimal paymentAmount;

    @NotNull(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede tener más de 255 caracteres")
    private String paymentDetailDescription;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDateTime paymentDetailsDate;

    @NotNull(message = "El módulo es obligatorio")
    @Size(max = 30, message = "El módulo no puede tener más de 30 caracteres")
    private String moduleType;

    @NotNull(message = "El ID de la cuenta contable es obligatorio")
    private Integer accountId;

}
