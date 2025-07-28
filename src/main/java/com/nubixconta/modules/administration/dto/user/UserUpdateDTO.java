package com.nubixconta.modules.administration.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {


    @Size(max = 50, message = "El firstName no puede tener mas de 50 caracteres")
    private String firstName;


    @Size(max = 50, message = "El lastName  no puede tener mas de 50 caracteres")
    private String lastName;


    @Size(max = 32, message = "La userName no puede tener mas de 32 caracteres")
    private String userName;


    @Size(max = 50, message = "La email no puede tener mas de 50 caracteres")
    @Email(message = "El correo no tiene un formato válido")
    private String email;


    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;


    @Size(max = 255, message = "El url de la photo no puede tener mas de 255 caracteres")
    private String photo;

    private Boolean status;

    private Boolean role;

}
