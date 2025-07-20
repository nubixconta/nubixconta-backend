package com.nubixconta.modules.administration.dto.user;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;



@Data
public class UserCreateDTO {

    @NotNull(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El firstName no puede tener mas de 50 caracteres")
    private String firstName;

    @NotNull(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El lastName  no puede tener mas de 50 caracteres")
    private String lastName;

    @NotNull(message = "El userName es obligatorio")
    @Size(max = 32, message = "La userName no puede tener mas de 32 caracteres")
    private String userName;

    @NotNull(message = "El correo es obligatorio")
    @Size(max = 50, message = "La email no puede tener mas de 50 caracteres")
    @Email(message = "El correo no tiene un formato válido")
    private String email;

    @NotNull(message = "La contraseña es obligatorio")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;


    @Size(max = 255, message = "El url de la photo no puede tener mas de 255 caracteres")
    private String photo;

    @NotNull(message = "El estado es obligatorio")
    private Boolean status;

    @NotNull(message = "El estado es obligatorio")
    private Boolean role;

}

