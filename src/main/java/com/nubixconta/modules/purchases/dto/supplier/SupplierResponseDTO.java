package com.nubixconta.modules.purchases.dto.supplier;

import com.nubixconta.modules.sales.entity.PersonType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SupplierResponseDTO {
    private Integer idSupplier;
    private String supplierName;
    private String supplierLastName;
    private String supplierDui;
    private String supplierNit;
    private String nrc;
    private String address;
    private String email;
    private String phone;
    private Integer creditDay;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private Boolean status;
    private Boolean exemptFromVat;
    private String businessActivity;
    private PersonType personType;
    private Boolean appliesPerception;
    private String supplierType;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
}