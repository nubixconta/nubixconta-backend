package com.nubixconta.modules.purchases.dto.creditnote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseCreditNoteUpdateDTO {
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    private String documentNumber;

    @Size(max = 255, message = "La descripción puede tener máximo 255 caracteres")
    private String description;

    private LocalDateTime issueDate;

    @Digits(integer = 10, fraction = 2)
    private BigDecimal subtotalAmount;

    @Digits(integer = 10, fraction = 2)
    private BigDecimal vatAmount;

    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalAmount;

    private List<@Valid PurchaseCreditNoteDetailCreateDTO> details;
}