package com.nubixconta.modules.administration.dto.company;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyCreateDTO {



    private Integer userId;

    private Integer accountId;

    @NotNull(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String companyName;

    @Size(max = 10, message = "El DUI no puede tener más de 10 caracteres")
    private String companyDui;

    @Size(max = 17, message = "El NIT no puede tener más de 17 caracteres")
    private String companyNit;

    @Size(max = 14, message = "El NRC no puede tener más de 14 caracteres")
    private String companyNrc;

    @NotNull(message = "La fecha de creación es obligatoria")
    private LocalDateTime creationDate;

    @Size(max = 512, message = "El turn no puede superar los 512 caracteres")
    @NotNull(message = "El turn es obligatorio")
    private String turnCompany;

    @NotNull(message = "La address es obligatoria")
    @Size(max = 100, message = "La dirección puede tener máximo 50 caracteres")
    private String address;
}
