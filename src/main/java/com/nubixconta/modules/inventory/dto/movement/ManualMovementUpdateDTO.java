package com.nubixconta.modules.inventory.dto.movement;
import com.nubixconta.modules.inventory.entity.MovementType;
import lombok.Getter;
import lombok.Setter;

// Nota: Este DTO es idéntico al de creación, pero es bueno mantenerlo separado
// por si en el futuro las reglas de negocio para editar cambian.
@Getter
@Setter
public class ManualMovementUpdateDTO {

    // Todos los campos ahora son opcionales. El usuario puede enviar solo uno si quiere.
    private Integer productId;
    private Integer quantity;
    private MovementType movementType;
    private String description;
}
