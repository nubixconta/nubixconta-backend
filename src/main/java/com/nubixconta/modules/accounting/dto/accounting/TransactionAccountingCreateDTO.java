package com.nubixconta.modules.accounting.dto.accounting;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class TransactionAccountingCreateDTO {

    @NotNull(message = "La fecha de la transacción es obligatoria.")
    private LocalDateTime transactionDate;

    @NotEmpty(message = "La descripción no puede estar vacía.")
    private String description;

    @Valid // Asegura que se validen los DTOs anidados en la lista.
    @NotEmpty(message = "La transacción debe tener al menos una línea de asiento.")
    @Size(min = 2, message = "Una partida contable debe tener al menos dos líneas (un debe y un haber).")
    private Set<AccountingEntryDTO> entries;
}