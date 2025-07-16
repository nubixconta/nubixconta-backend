package com.nubixconta.modules.sales.dto.customer;

import com.nubixconta.modules.sales.entity.PersonType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class CustomerUpdateDTO {

    @NotBlank
    @Size(max = 50)
    private String customerName;

    @Size(max = 50)
    private String customerLastName;

    @Size(max = 10)
    private String customerDui;

    @Size(max = 17)
    private String customerNit;

    @Size(max = 14)
    private String ncr;

    @NotBlank
    @Size(max = 50)
    private String address;

    @NotBlank
    @Email
    @Size(max = 30)
    private String email;

    @NotBlank
    @Size(max = 8)
    private String phone;

    @NotNull
    @Min(0)
    private Integer creditDay;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    private BigDecimal creditLimit;

    @NotNull
    private Boolean exemptFromVat;

    @NotBlank
    @Size(max = 100)
    private String businessActivity;

    @NotNull(message = "El tipo de persona es obligatorio")
    private PersonType personType;

    @NotNull
    private Boolean appliesWithholding;

    @NotNull(message = "El estado es obligatorio")
    private Boolean status;
}
