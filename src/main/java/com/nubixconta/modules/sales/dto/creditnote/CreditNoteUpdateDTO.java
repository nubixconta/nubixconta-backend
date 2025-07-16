package com.nubixconta.modules.sales.dto.creditnote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
public class CreditNoteUpdateDTO {
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    private String documentNumber;

    @Size(max = 255, message = "La descripcion puede tener máximo 255 caracteres")
    private String description;

    private BigDecimal totalAmount;
    // --- ¡AÑADIR ESTOS DOS CAMPOS NUEVOS! ---
    private BigDecimal subtotalAmount;
    private BigDecimal vatAmount;//mandar cero si no hay

    private List<@Valid CreditNoteDetailCreateDTO> details;
}