package com.nubixconta.modules.accounting.dto.catalog;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class ActivateAccountsDTO {
    @NotEmpty(message = "La lista de IDs de cuentas maestras no puede estar vacía.")
    private List<Integer> masterAccountIds;
}