package com.nubixconta.modules.sales.dto.creditnote;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
public class CreditNoteUpdateDTO {
    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    private String documentNumber;

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "PENDIENTE|APLICADA|ANULADA", message = "El estado debe ser PENDIENTE, APLICADA o ANULADA")
    private String creditNoteStatus;

    @NotNull(message = "Debe especificar al menos un detalle")
    @Size(min = 1, message = "Debe haber al menos un detalle")
    private List<@Valid CreditNoteDetailCreateDTO> details;
}