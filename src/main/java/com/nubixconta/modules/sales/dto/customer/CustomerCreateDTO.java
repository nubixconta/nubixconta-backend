package com.nubixconta.modules.sales.dto.customer;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class CustomerCreateDTO {
    @NotBlank(message = "El nombre es obligatorio")
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

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 50)
    private String address;

    @NotBlank(message = "El email es obligatorio")
    @Email
    @Size(max = 30)
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 8)
    private String phone;

    @NotNull(message = "El número de días de crédito es obligatorio")
    @Min(0)
    private Integer creditDay;

    @NotNull(message = "El límite de crédito es obligatorio")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal creditLimit;

    @NotNull(message = "El campo de exención de IVA es obligatorio")
    private Boolean exemptFromVat;

    @NotBlank(message = "La actividad económica es obligatoria")
    @Size(max = 100)
    private String businessActivity;

    @NotBlank(message = "El tipo de persona es obligatorio")
    @Size(max = 30)
    private String personType;

    @NotNull(message = "El campo de retención es obligatorio")
    private Boolean appliesWithholding;
}
