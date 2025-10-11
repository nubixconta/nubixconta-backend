package com.nubixconta.modules.purchases.dto.purchases;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseUpdateDTO {

    @Size(max = 20, message = "El número de documento no puede exceder los 20 caracteres")
    private String documentNumber;

    private LocalDateTime issueDate;

    @Digits(integer = 10, fraction = 2)
    private BigDecimal subtotalAmount;

    @Digits(integer = 10, fraction = 2)
    private BigDecimal vatAmount;

    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalAmount;

    @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres")
    private String purchaseDescription;

    private List<@Valid PurchaseDetailCreateDTO> purchaseDetails;
}