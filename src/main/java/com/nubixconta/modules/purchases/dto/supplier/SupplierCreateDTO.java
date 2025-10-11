package com.nubixconta.modules.purchases.dto.supplier;

import com.nubixconta.modules.sales.entity.PersonType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SupplierCreateDTO {

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 100, message = "El nombre puede tener máximo 100 caracteres")
    private String supplierName;

    @Size(max = 100, message = "El apellido puede tener máximo 100 caracteres")
    private String supplierLastName;

    @Size(max = 10, message = "El DUI puede tener máximo 10 caracteres")
    private String supplierDui;

    @Size(max = 17, message = "El NIT puede tener máximo 17 caracteres")
    private String supplierNit;

    @NotBlank(message = "El NRC es obligatorio")
    @Size(max = 14, message = "El NRC puede tener máximo 14 caracteres")
    private String nrc;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 50, message = "La dirección puede tener máximo 50 caracteres")
    private String address;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 30, message = "El email puede tener máximo 30 caracteres")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 8, message = "El teléfono puede tener máximo 8 caracteres")
    private String phone;

    @NotNull(message = "El número de días de crédito es obligatorio")
    @Min(value = 0, message = "Los días de crédito no pueden ser negativos")
    private Integer creditDay;

    @NotNull(message = "El límite de crédito es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "Formato de límite de crédito inválido")
    private BigDecimal creditLimit;

    @NotNull(message = "El campo de exención de IVA es obligatorio")
    private Boolean exemptFromVat;

    @NotBlank(message = "La actividad económica es obligatoria")
    @Size(max = 100, message = "La actividad económica puede tener máximo 100 caracteres")
    private String businessActivity;

    @NotNull(message = "El tipo de persona es obligatorio")
    private PersonType personType;

    @NotNull(message = "El campo 'aplica percepción' es obligatorio")
    private Boolean appliesPerception;

    @Size(max = 20, message = "El tipo de proveedor puede tener máximo 20 caracteres")
    private String supplierType;
}