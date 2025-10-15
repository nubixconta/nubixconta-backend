package com.nubixconta.modules.inventory.dto.movement;

import com.nubixconta.modules.inventory.entity.InventoryMovement;
import com.nubixconta.modules.inventory.entity.MovementStatus;
import com.nubixconta.modules.inventory.entity.MovementType;
import com.nubixconta.modules.sales.entity.Customer; // Asegúrate de tener este import
import com.nubixconta.modules.purchases.entity.Supplier; // Y este también

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

    // --- CAMPOS CORREGIDOS PARA TERCEROS INVOLUCRADOS ---
    // Solo uno de estos dos campos tendrá valor a la vez.
    private String customerName;  // Para Ventas y Notas de Crédito
    private String supplierName; // Para Compras

    // Información del producto afectado
    private MovementProductInfoDTO product;


    /**
     * MÉTODO DE FÁBRICA CORREGIDO.
     * Convierte una entidad InventoryMovement a este DTO de forma semánticamente correcta.
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

        // Lógica corregida para determinar el origen y el tercero involucrado
        if (movement.getSale() != null) {
            dto.setOriginModule("Ventas");
            dto.setOriginDocument(movement.getSale().getDocumentNumber());
            Customer customer = movement.getSale().getCustomer();
            // Asigna el nombre al campo correcto
            dto.setCustomerName(buildFullNameFrom(movement.getSale().getCustomer()));

        } else if (movement.getCreditNote() != null) {
            dto.setOriginModule("Ventas (Nota de Crédito)");
            dto.setOriginDocument(movement.getCreditNote().getDocumentNumber());
            // Asigna el nombre al campo correcto
            dto.setCustomerName(buildFullNameFrom(movement.getCreditNote().getSale().getCustomer()));

        } else if (movement.getPurchase() != null) {
            dto.setOriginModule("Compras");
            dto.setOriginDocument(movement.getPurchase().getDocumentNumber());
            dto.setSupplierName(buildFullNameFrom(movement.getPurchase().getSupplier()));

        } else {
            dto.setOriginModule("Ajuste Manual de Inventario");
            dto.setOriginDocument("Movimiento #" + movement.getMovementId());
            // Para ajustes manuales, ambos nombres de terceros son nulos por defecto.
        }

        return dto;
    }
    /**
     * NUEVO MÉTODO DE AYUDA PRIVADO.
     * Recrea la lógica de getFullName pero dentro del DTO.
     * Es estático porque no depende del estado de una instancia de MovementResponseDTO.
     */
    private static String buildFullNameFrom(Customer customer) {
        if (customer == null) {
            return null; // O un valor por defecto como "Cliente no disponible"
        }
        // Accedemos a los getters públicos de la entidad Customer
        if (customer.getCustomerLastName() != null && !customer.getCustomerLastName().isBlank()) {
            return customer.getCustomerName() + " " + customer.getCustomerLastName();
        }
        return customer.getCustomerName();
    }

    /**
     * NUEVO MÉTODO DE AYUDA PRIVADO.
     * Recrea la lógica de getFullName para un Proveedor.
     */
    private static String buildFullNameFrom(Supplier supplier) {
        if (supplier == null) {
            return null;
        }
        // Accedemos a los getters públicos de la entidad Supplier
        if (supplier.getSupplierLastName() != null && !supplier.getSupplierLastName().isBlank()) {
            return supplier.getSupplierName() + " " + supplier.getSupplierLastName();
        }
        return supplier.getSupplierName();
    }
}