package com.nubixconta.modules.administration.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
//DTO para que administrador cambie su propia contraseña
public class ChangePasswordDTO {
    @NotNull(message = "La contraseña anterior es obligatoria.")
    private String oldPassword;

    @NotNull(message = "La nueva contraseña es obligatoria.")
    @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres.")
    private String newPassword;

    @NotNull(message = "La confirmación de la contraseña es obligatoria.")
    private String confirmPassword;
}
