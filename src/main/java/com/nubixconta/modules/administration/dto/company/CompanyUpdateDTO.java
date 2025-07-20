package com.nubixconta.modules.administration.dto.company;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyUpdateDTO {


    private Integer userId;


    private Integer accountId;

    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String companyName;

    @Size(max = 10, message = "El DUI no puede tener más de 10 caracteres")
    private String companyDui;

    @Size(max = 17, message = "El NIT no puede tener más de 17 caracteres")
    private String companyNit;

    @Size(max = 14, message = "El NRC no puede tener más de 14 caracteres")
    private String companyNrc;


    private Boolean companyStatus;


    private Boolean activeStatus;

}
