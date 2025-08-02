package com.nubixconta.modules.inventory.entity;

import com.nubixconta.modules.sales.entity.CreditNote;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.administration.entity.Company;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movement")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_id")
    private Integer movementId;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product", referencedColumnName = "id_product")
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @NotNull(message = "La cantidad es obligatoria")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Guardará el stock que quedó en el producto DESPUÉS de este movimiento.
    @NotNull(message = "El stock resultante es obligatorio")
    @Column(name = "stock_after_movement", nullable = false)
    private Integer stockAfterMovement;

    @CreationTimestamp
    @Column(name = "date", nullable = false, updatable = false)
    private LocalDateTime date;


    // Este es el estado PROPIO del movimiento (para ajustes manuales)
    @NotNull(message = "El estado del movimiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_status", nullable = false)
    private MovementStatus status;

    @Size(max = 256, message = "La descripción puede tener máximo 256 caracteres")
    @Column(name = "description", length = 256)
    private String description;

    // --- VÍNCULOS DE AUDITORÍA ---
    // Si el movimiento es por una venta, este campo tendrá valor.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = true)
    private Sale sale;

    // Si es por una nota de crédito, este tendrá valor.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id", nullable = true)
    private CreditNote creditNote;


}