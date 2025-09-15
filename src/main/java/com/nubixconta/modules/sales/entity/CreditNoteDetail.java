package com.nubixconta.modules.sales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.nubixconta.modules.inventory.entity.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects; // <-- Asegúrate de importar esto

@Entity
@Table(name = "credit_note_detail")
@Getter
@Setter
@NoArgsConstructor
public class CreditNoteDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_note_detail_id")
    private Integer creditNoteDetailId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY) // Sugerencia: Usar LAZY fetching
    @JoinColumn(name = "credit_note_id", nullable = false)
    private CreditNote creditNote;

    @ManyToOne(fetch = FetchType.LAZY) // Sugerencia: Usar LAZY fetching
    @JoinColumn(name = "id_product", referencedColumnName = "id_product")
    private Product product;

    @Size(max = 50, message = "El nombre del servicio puede tener máximo 50 caracteres")
    @Column(name = "service_name", length = 50)
    private String serviceName;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "El precio unitario es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El precio unitario debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @NotNull(message = "El subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El subtotal debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;


    // --- ¡AÑADIMOS equals() y hashCode() CRUCIALES! ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditNoteDetail that = (CreditNoteDetail) o;

        // La igualdad de un detalle se define por su padre (la nota de crédito)
        // y su clave de negocio (el producto o el servicio).
        return Objects.equals(creditNote, that.creditNote) &&
                Objects.equals(product, that.product) &&
                Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        // El hash code debe basarse en los mismos campos que equals.
        return Objects.hash(creditNote, product, serviceName);
    }
}