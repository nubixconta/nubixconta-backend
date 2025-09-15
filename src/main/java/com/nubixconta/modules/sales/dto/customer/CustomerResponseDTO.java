package com.nubixconta.modules.sales.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.nubixconta.modules.sales.entity.PersonType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerResponseDTO {
    private Integer clientId;
    private String customerName;
    private String customerLastName;
    private String customerDui;
    private String customerNit;
    private String ncr;
    private String address;
    private String email;
    private String phone;
    private Integer creditDay;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private Boolean exemptFromVat;
    private String businessActivity;
    private PersonType personType;
    private Boolean appliesWithholding;
    private Boolean status;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
}
