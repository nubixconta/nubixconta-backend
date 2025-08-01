package com.nubixconta.modules.administration.dto.AccessLog;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccessLogUpdateDTO {
    @NotNull(message = "El ID del registro de acceso es obligatorio")
    private Long accessLogId;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime dateEnd;
}