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
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    private String documentNumber;

    private List<@Valid CreditNoteDetailCreateDTO> details;
}