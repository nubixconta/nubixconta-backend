package com.nubixconta.modules.accounting.dto.catalog;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class DeactivateAccountsDTO {
    @NotEmpty(message = "La lista de IDs de catálogo a desactivar no puede estar vacía.")
    private List<Integer> catalogIds;
}