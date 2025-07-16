package com.nubixconta.modules.administration.dto.company;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class CompanyResponseDTO {

    private String companyName;
    private String companyDui;
    private String companyNit;
    private String companyNrc;
    private Boolean companyStatus;
    private Boolean activeStatus;
    private LocalDateTime creationDate;

}
