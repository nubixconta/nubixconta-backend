package com.nubixconta.modules.accountsreceivable.dto.collectiondetail;

import jakarta.persistence.Column;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CollectionDetailUpdateDTO {


    private Integer accountReceivableId;

    @Size(max = 10, message = "El estado no puede tener más 10 caracteres")
    @Column(name = "payment_status", length = 10)
    private String paymentStatus;

    @Size(max = 30, message = "La referencia no puede tener más de 30 caracteres")
    private String reference;


    @Size(max = 20, message = "El método no puede tener más de 20 caracteres")
    private String paymentMethod;


    @Digits(integer = 8, fraction = 2, message = "El monto puede tener hasta 8 dígitos enteros y 2 decimales")
    @DecimalMin(value = "0.00", inclusive = true, message = "El monto no puede ser negativo")
    private BigDecimal paymentAmount;


    @Size(max = 255, message = "La descripción no puede tener más de 255 caracteres")
    private String paymentDetailDescription;


    private Integer accountId;
}
