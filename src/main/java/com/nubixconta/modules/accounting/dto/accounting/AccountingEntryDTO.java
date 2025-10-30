package com.nubixconta.modules.accounting.dto.accounting;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountingEntryDTO {

    @NotNull(message = "El ID del catálogo de cuenta es obligatorio.")
    private Integer catalogId;

    @NotNull(message = "El campo 'debe' es obligatorio (puede ser 0).")
    private BigDecimal debit;

    @NotNull(message = "El campo 'haber' es obligatorio (puede ser 0).")
    private BigDecimal credit;

    // La descripción es opcional a nivel de línea en el DTO,
    // ya que se copiará de la cabecera en el servicio.
    private String description;
}