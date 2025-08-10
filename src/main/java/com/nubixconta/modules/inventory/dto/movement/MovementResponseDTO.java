package com.nubixconta.modules.inventory.dto.movement;

import com.nubixconta.modules.inventory.entity.InventoryMovement;
import com.nubixconta.modules.inventory.entity.MovementStatus;
import com.nubixconta.modules.inventory.entity.MovementType;
import com.nubixconta.modules.sales.entity.Customer;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class MovementResponseDTO {

    private Integer movementId;
    private MovementType movementType;
    private Integer quantity;
    private Integer stockAfterMovement;
    private LocalDateTime date;
    private MovementStatus status;
    private String description;

    // Información del origen del movimiento
    private String originModule;
    private String originDocument;
    private String customerName;

    // Información del producto afectado
    private MovementProductInfoDTO product;


    /**
     * Método de fábrica estático para convertir una entidad InventoryMovement a este DTO.
     * Esto mantiene la lógica de mapeo encapsulada y limpia.
     */
    public static MovementResponseDTO fromEntity(InventoryMovement movement) {
        MovementResponseDTO dto = new MovementResponseDTO();
        dto.setMovementId(movement.getMovementId());
        dto.setMovementType(movement.getMovementType());
        dto.setQuantity(movement.getQuantity());
        dto.setStockAfterMovement(movement.getStockAfterMovement());
        dto.setDate(movement.getDate());
        dto.setStatus(movement.getStatus());
        dto.setDescription(movement.getDescription());

        dto.setProduct(new MovementProductInfoDTO(
                movement.getProduct().getIdProduct(),
                movement.getProduct().getProductCode(),
                movement.getProduct().getProductName()
        ));

        // --- INICIO DE LA LÓGICA MODIFICADA ---
        Customer customer = null;

        // Lógica para determinar el origen
        if (movement.getSale() != null) {
            dto.setOriginModule("Ventas");
            dto.setOriginDocument(movement.getSale().getDocumentNumber());
            // Accedemos al cliente a través de la relación con la Venta
            customer = movement.getSale().getCustomer();
        } else if (movement.getCreditNote() != null) {
            dto.setOriginModule("Ventas (Nota de Crédito)");
            dto.setOriginDocument(movement.getCreditNote().getDocumentNumber());
            // Accedemos al cliente a través de la Nota de Crédito, que a su vez tiene una Venta
            customer = movement.getCreditNote().getSale().getCustomer();
        } else {
            dto.setOriginModule("Ajuste Manual de Inventario");
            dto.setOriginDocument("Movimiento #" + movement.getMovementId());
            // Los ajustes manuales no tienen cliente, por lo que 'customer' se mantiene null.
        }

        // Ahora, poblamos el nombre del cliente basado en lo que encontramos
        if (customer != null) {
            // Unimos nombre y apellido para tener el nombre completo, si el apellido existe.
            String fullName = customer.getCustomerName();
            if (customer.getCustomerLastName() != null && !customer.getCustomerLastName().isBlank()) {
                fullName += " " + customer.getCustomerLastName();
            }
            dto.setCustomerName(fullName);
        } else {
            // Si no hay cliente (ajuste manual), ponemos un valor por defecto.
            dto.setCustomerName("N/A");
        }
        // --- FIN DE LA LÓGICA MODIFICADA ---

        return dto;
    }
}