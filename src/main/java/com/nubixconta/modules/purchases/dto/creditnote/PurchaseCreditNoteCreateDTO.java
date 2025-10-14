package com.nubixconta.modules.purchases.dto.creditnote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseCreditNoteCreateDTO {

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    private String documentNumber;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción puede tener máximo 255 caracteres")
    private String description;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDateTime issueDate;

    @NotNull(message = "La compra asociada es obligatoria")
    private Integer purchaseId;

    @NotNull(message = "El monto subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal subtotalAmount;

    @NotNull(message = "El monto de IVA es obligatorio")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal vatAmount;

    @NotNull(message = "El monto total es obligatorio")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalAmount;

    @NotEmpty(message = "Debe especificar al menos un detalle")
    private List<@Valid PurchaseCreditNoteDetailCreateDTO> details;
}