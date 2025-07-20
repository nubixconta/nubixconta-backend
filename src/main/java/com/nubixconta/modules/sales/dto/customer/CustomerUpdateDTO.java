package com.nubixconta.modules.sales.dto.customer;

import com.nubixconta.modules.sales.entity.PersonType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerUpdateDTO {

    private String customerName;
    private String customerLastName;
    private String customerDui;
    private String customerNit;
    private String ncr;
    private String address;
    @Email
    @Size(max = 30)
    private String email;
    @Size(max = 8)
    private String phone;


    @Min(0)
    private Integer creditDay;


    @Digits(integer = 10, fraction = 2)
    private BigDecimal creditLimit;

    private Boolean exemptFromVat;


    @Size(max = 100)
    private String businessActivity;

    private PersonType personType;


    private Boolean appliesWithholding;

    private Boolean status;
}
