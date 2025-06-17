package com.nubixconta.modules.sales.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
@Data
public class Customer {
    @Id
    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "supplier_name", length = 100, nullable = false)
    private String supplierName;

    @Column(name = "supplier_dui", length = 10)
    private String supplierDui;

    @Column(name = "supplier_nit", length = 17)
    private String supplierNit;

    @Column(name = "ncr", length = 14)
    private String ncr;

    @Column(name = "address", length = 50)
    private String address;

    @Column(name = "email", length = 30)
    private String email;

    @Column(name = "phone", length = 8)
    private String phone;

    @Column(name = "credit_day")
    private Integer creditDay;

    @Column(name = "credit_limit", precision = 10, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "exempt_from_vat")
    private Boolean exemptFromVat;

    @Column(name = "business_activity", length = 100)
    private String businessActivity;

    @Column(name = "person_type", length = 30)
    private String personType;

    @Column(name = "applies_withholding")
    private Boolean appliesWithholding;
}