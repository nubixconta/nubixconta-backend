package com.nubixconta.modules.purchases.dto.purchases;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseCreateDTO {

    @NotNull(message = "El ID del proveedor es obligatorio")
    private Integer supplierId;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento no puede exceder los 20 caracteres")
    private String documentNumber;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDateTime issueDate;

    @NotNull(message = "El monto subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal subtotalAmount;

    @NotNull(message = "El monto de IVA es obligatorio")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal vatAmount;

    @NotNull(message = "El monto total es obligatorio")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalAmount;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres")
    private String purchaseDescription;

    @NotBlank(message = "El tipo de módulo es obligatorio")
    @Size(max = 30)
    private String moduleType;

    @NotEmpty(message = "La compra debe tener al menos un detalle")
    private List<@Valid PurchaseDetailCreateDTO> purchaseDetails;
}