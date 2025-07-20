package com.nubixconta.modules.administration.dto.company;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.administration.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyCreateDTO {


    @NotNull(message = "El usuario es obligatorio")
    private Integer userId;

    @NotNull(message = "La cuenta es obligatorio")
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

    @NotNull(message = "El estado de la empresa es obligatorio")
    private Boolean companyStatus;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activeStatus;

    @NotNull(message = "La fecha de creación es obligatoria")
    private LocalDateTime creationDate;
}
