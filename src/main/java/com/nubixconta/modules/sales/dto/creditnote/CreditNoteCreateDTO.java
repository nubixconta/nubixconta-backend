package com.nubixconta.modules.sales.dto.creditnote;

import jakarta.persistence.Column;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
public class CreditNoteCreateDTO {
    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    private String documentNumber;

    @NotBlank(message = "La descripcion es obligatorio")
    @Size(max = 255, message = "La descripcion puede tener máximo 255 caracteres")
    @Column(name = "description", length = 255)
    private String description;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDateTime issueDate;


    @NotNull(message = "La venta asociada es obligatoria")
    private Integer saleId;

    @NotNull(message = "Debe especificar al menos un detalle")
    @Size(min = 1, message = "Debe haber al menos un detalle")
    private List<@Valid CreditNoteDetailCreateDTO> details;

    // --- ¡AÑADIR ESTOS TRES CAMPOS NUEVOS! ---

    @NotNull(message = "El monto total es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "Debe tener hasta 10 dígitos y 2 decimales")
    private BigDecimal totalAmount;

    // --- ¡AÑADIR ESTOS DOS CAMPOS NUEVOS! ---
    @NotNull(message = "El monto subtotal es obligatorio")
    private BigDecimal subtotalAmount;

    @NotNull(message = "El impuesto es obligatorio")
    private BigDecimal vatAmount;//mandar cero si no hay
}
