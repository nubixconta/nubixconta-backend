package com.nubixconta.modules.administration.dto.user;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDTO {

    private String firstName;


    private String lastName;

    private String userName;

    private String email;

    private String photo;

    private Boolean status;

}
