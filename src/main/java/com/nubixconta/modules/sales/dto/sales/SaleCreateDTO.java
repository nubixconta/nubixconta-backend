package com.nubixconta.modules.sales.dto.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SaleCreateDTO {

        @NotNull(message = "El ID del cliente es obligatorio")
        private Integer clientId;

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 20, message = "Máximo 20 caracteres")
        private String documentNumber;

        @NotNull(message = "La fecha de emisión es obligatoria")
        private LocalDateTime issueDate;

        @NotBlank(message = "El tipo de venta es obligatorio")
        @Size(max = 10, message = "Máximo 10 caracteres")
        private String saleType;

        @NotNull(message = "El monto total es obligatorio")
        @Digits(integer = 10, fraction = 2, message = "Debe tener hasta 10 dígitos y 2 decimales")
        private BigDecimal totalAmount;

        // --- ¡AÑADIR ESTOS DOS CAMPOS NUEVOS! ---
        @NotNull(message = "El monto subtotal es obligatorio")
        private BigDecimal subtotalAmount;

        @NotNull(message = "El impuesto es obligatorio")
        private BigDecimal vatAmount;//mandar cero si no hay

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "Máximo 255 caracteres")
        private String saleDescription;

        @NotBlank(message = "El módulo es obligatorio")
        @Size(max = 30, message = "Máximo 30 caracteres")
        private String moduleType;

        @NotEmpty(message = "Debe ingresar al menos un detalle de venta")
        private List<@Valid SaleDetailCreateDTO> saleDetails;
}

