package com.nubixconta.modules.purchases.dto.supplier;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SupplierUpdateDTO {

    @Size(max = 100, message = "El nombre puede tener máximo 100 caracteres")
    private String supplierName;

    @Size(max = 100, message = "El apellido puede tener máximo 100 caracteres")
    private String supplierLastName;

    @Size(max = 50, message = "La dirección puede tener máximo 50 caracteres")
    private String address;

    @Email(message = "El email debe tener un formato válido")
    @Size(max = 30, message = "El email puede tener máximo 30 caracteres")
    private String email;

    @Size(max = 8, message = "El teléfono puede tener máximo 8 caracteres")
    private String phone;

    @Min(value = 0, message = "Los días de crédito no pueden ser negativos")
    private Integer creditDay;

    @Digits(integer = 10, fraction = 2, message = "Formato de límite de crédito inválido")
    private BigDecimal creditLimit;

    private Boolean exemptFromVat;

    @Size(max = 100, message = "La actividad económica puede tener máximo 100 caracteres")
    private String businessActivity;

    private Boolean appliesPerception;

    @Size(max = 20, message = "El tipo de proveedor puede tener máximo 20 caracteres")
    private String supplierType;
}