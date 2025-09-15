package com.nubixconta.modules.inventory.dto.product;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ProductCreateDTO {
    @NotBlank(message = "El código de producto es obligatorio")
    @Size(max = 100, message = "El código puede tener máximo 100 caracteres")
    private String productCode;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 256, message = "El nombre puede tener máximo 256 caracteres")
    private String productName;

    @NotBlank(message = "La unidad es obligatoria")
    @Size(max = 50, message = "La unidad puede tener máximo 50 caracteres")
    private String unit;

    @NotNull(message = "La cantidad en stock es obligatoria")
    @Min(value = 0, message = "La cantidad en stock no puede ser negativa")
    private Integer stockQuantity;
}
