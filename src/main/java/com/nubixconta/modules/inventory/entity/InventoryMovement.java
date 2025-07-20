package com.nubixconta.modules.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movement")
@Data
public class InventoryMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_id")
    private Integer movementId;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_product", referencedColumnName = "id_product")
    private Product product;

    @NotNull(message = "La fecha es obligatoria")
    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @NotBlank(message = "El tipo de movimiento es obligatorio")
    @Size(max = 30, message = "El tipo de movimiento puede tener máximo 30 caracteres")
    @Column(name = "movement_type", length = 30, nullable = false)
    private String movementType;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 256, message = "La descripción puede tener máximo 256 caracteres")
    @Column(name = "movement_description", length = 256, nullable = false)
    private String movementDescription;

    @NotBlank(message = "El módulo es obligatorio")
    @Size(max = 50, message = "El módulo puede tener máximo 50 caracteres")
    @Column(name = "module", length = 50, nullable = false)
    private String module;

}