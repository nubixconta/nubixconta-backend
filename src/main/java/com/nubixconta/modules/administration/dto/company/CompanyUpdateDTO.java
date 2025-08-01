package com.nubixconta.modules.administration.dto.company;


import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyUpdateDTO {

    @Min(value = 1, message = "El ID del usuario debe ser mayor que cero")
    private Integer userId;

    @Min(value = 1, message = "El ID del usuario debe ser mayor que cero")
    private Integer accountId;

    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String companyName;

    @Size(max = 10, message = "El DUI no puede tener más de 10 caracteres")
    private String companyDui;

    @Size(max = 17, message = "El NIT no puede tener más de 17 caracteres")
    private String companyNit;

    @Size(max = 14, message = "El NRC no puede tener más de 14 caracteres")
    private String companyNrc;


    @Size(max = 100, message = "La dirección puede tener máximo 100 caracteres")
    private String address;

    private Boolean companyStatus;

    @Size(max = 512, message = "El turn no puede superar los 512 caracteres")
    private String turnCompany;


    private Boolean activeStatus;

}
