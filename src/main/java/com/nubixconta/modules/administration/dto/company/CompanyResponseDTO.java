package com.nubixconta.modules.administration.dto.company;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class CompanyResponseDTO {

    private Integer id;
    private Integer userId;
    private String companyName;
    private String companyDui;
    private String companyNit;
    private String companyNrc;
    private Boolean companyStatus;
    private Boolean activeStatus;
    private LocalDateTime creationDate;
    private String address;
    private String turnCompany;
    private String imageUrl;

}
