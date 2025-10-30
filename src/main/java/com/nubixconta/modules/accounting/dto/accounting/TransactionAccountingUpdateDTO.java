package com.nubixconta.modules.accounting.dto.accounting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class TransactionAccountingUpdateDTO {

    // Todos los campos son opcionales para una actualización parcial (PATCH),
    // pero si se envían, se validan.

    private LocalDateTime transactionDate;

    private String description;

    @Valid
    @Size(min = 2, message = "Una partida contable debe tener al menos dos líneas.")
    private Set<AccountingEntryDTO> entries;
}