package com.nubixconta.modules.inventory.dto.movement;

import com.nubixconta.modules.inventory.entity.MovementType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualMovementCreateDTO {

    @NotNull(message = "El ID del producto es obligatorio.")
    private Integer productId;

    @NotNull(message = "La cantidad es obligatoria.")
    @Positive(message = "La cantidad debe ser un número positivo.")
    private Integer quantity;

    @NotNull(message = "El tipo de movimiento (IN/OUT) es obligatorio.")
    private MovementType movementType;

    @NotBlank(message = "La descripción es obligatoria para movimientos manuales.")
    @Size(max = 256, message = "La descripción no puede exceder los 256 caracteres.")
    private String description;
}