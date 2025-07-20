package com.nubixconta.modules.sales.entity;

import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id")
    private Integer clientId;

    @NotNull(message = "El userId es obligatorio")
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre puede tener máximo 50 caracteres")
    @Column(name = "customer_name", length = 50, nullable = false)
    private String customerName;


    @Size(max = 50, message = "El apellido puede tener máximo 50 caracteres")
    @Column(name = "customer_last_name", length = 50)
    private String customerLastName;


    @Size(max = 10, message = "El DUI puede tener máximo 10 caracteres")
    @Column(name = "customer_dui", length = 10)
    private String customerDui;


    @Size(max = 17, message = "El NIT puede tener máximo 17 caracteres")
    @Column(name = "customer_nit", length = 17)
    private String customerNit;

    @Size(max = 14, message = "El NCR puede tener máximo 14 caracteres")
    @Column(name = "ncr", length = 14)
    private String ncr;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 50, message = "La dirección puede tener máximo 50 caracteres")
    @Column(name = "address", length = 50, nullable = false)
    private String address;

    @NotBlank(message = "El email es obligatorio")
    @Size(max = 30, message = "El email puede tener máximo 30 caracteres")
    @Email(message = "El email debe tener un formato válido")
    @Column(name = "email", length = 30, nullable = false)
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 8, message = "El teléfono puede tener máximo 8 caracteres")
    @Column(name = "phone", length = 8, nullable = false)
    private String phone;

    @NotNull(message = "El número de días de crédito es obligatorio")
    @Min(value = 0, message = "El número de días de crédito no puede ser negativo")
    @Column(name = "credit_day", nullable = false)
    private Integer creditDay;

    @NotNull(message = "El límite de crédito es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El límite de crédito debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "credit_limit", precision = 10, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    @NotNull(message = "El estado es obligatorio")
    @Column(name = "status", nullable = false)
    private Boolean status;

    @NotNull(message = "La fecha de creación es obligatoria")
    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @NotNull(message = "El campo de exención de IVA es obligatorio")
    @Column(name = "exempt_from_vat", nullable = false)
    private Boolean exemptFromVat;

    @NotBlank(message = "La actividad económica es obligatoria")
    @Size(max = 100, message = "La actividad económica puede tener máximo 100 caracteres")
    @Column(name = "business_activity", length = 100, nullable = false)
    private String businessActivity;

    @NotBlank(message = "El tipo de persona es obligatorio")
    @Size(max = 30, message = "El tipo de persona puede tener máximo 30 caracteres")
    @Column(name = "person_type", length = 30, nullable = false)
    private String personType;

    @NotNull(message = "El campo de retención es obligatorio")
    @Column(name = "applies_withholding", nullable = false)
    private Boolean appliesWithholding;
}
