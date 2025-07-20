package com.nubixconta.modules.administration.dto.changehistory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChangeHistoryCreateDTO {

    @NotNull(message = "El id de userId es obligatorio")
    private Integer userId;


    private Integer companyId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDateTime date;

    @NotNull(message = "Es obligatoria la acción que se realizó")
    @Size(max = 100, message = "La acción no puede tener más de 100 caracteres")
    private String actionPerformed;

    @NotNull(message = "El módulo es obligatorio")
    @Size(max = 50, message = "El nombre del módulo no puede tener más de 50 caracteres")
    private String moduleName;
}
