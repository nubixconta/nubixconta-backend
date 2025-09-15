package com.nubixconta.modules.administration.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
//DTO para que un administrador cambie la contraseña de un asistente.
public class AdminResetPasswordDTO {
    @NotNull(message = "La contraseña del administrador es obligatoria.")
    private String adminPassword;

    @NotNull(message = "La nueva contraseña es obligatoria.")
    @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres.")
    private String newPassword;

    @NotNull(message = "La confirmación de la contraseña es obligatoria.")
    private String confirmPassword;
}
