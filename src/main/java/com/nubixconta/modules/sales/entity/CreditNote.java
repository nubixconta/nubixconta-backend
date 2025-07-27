package com.nubixconta.modules.sales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "credit_note")
// REEMPLAZAMOS @Data por anotaciones específicas y seguras
@Getter
@Setter
@NoArgsConstructor
public class CreditNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_credit")
    private Integer idNotaCredit;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    @Column(name = "document_number", length = 20, nullable = false, unique = true) // Sugerencia: añadir unique=true
    private String documentNumber;

    @NotBlank(message = "La descripcion es obligatorio")
    @Size(max = 255, message = "La descripcion puede tener máximo 255 caracteres")
    @Column(name = "description", length = 255)
    private String description;

    @NotBlank(message = "El estado de la nota de crédito es obligatorio")
    @Size(max = 10, message = "El estado puede tener máximo 10 caracteres")
    @Column(name = "credit_note_status", length = 10, nullable = false)
    private String creditNoteStatus;

    @NotNull(message = "La fecha de emisión de la nota de crédito es obligatoria")
    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @NotNull(message = "La venta asociada es obligatoria")
    // La relación ahora es ManyToOne, ya que muchas NC pueden apuntar a una Venta.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    // El JoinColumn ya no necesita 'unique = true'.
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;


    // --- ¡AÑADIR ESTOS TRES CAMPOS NUEVOS! ---

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "subtotal_amount", nullable = false)
    private BigDecimal subtotalAmount;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "vat_amount", nullable = false)
    private BigDecimal vatAmount;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    // --- ¡AÑADIMOS LA RELACIÓN CON LOS DETALLES! ---
    @OneToMany(
            mappedBy = "creditNote", // Este será el nombre del campo en la entidad CreditNoteDetail
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<CreditNoteDetail> details = new HashSet<>();


    // --- ¡AÑADIMOS LOS MÉTODOS HELPER! ---
    public void addDetail(CreditNoteDetail detail) {
        if (this.details == null) {
            this.details = new HashSet<>();
        }
        this.details.add(detail);
        detail.setCreditNote(this); // Sincroniza el lado inverso de la relación
    }

    public void removeDetail(CreditNoteDetail detail) {
        if (this.details != null) {
            this.details.remove(detail);
            detail.setCreditNote(null);
        }
    }

    // --- ¡AÑADIMOS equals() y hashCode() SEGUROS PARA JPA! ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreditNote that)) return false;
        // Para entidades, la igualdad se basa en el ID si no es nulo
        return idNotaCredit != null && idNotaCredit.equals(that.idNotaCredit);
    }

    @Override
    public int hashCode() {
        // Usar una constante (el hash de la clase) es la mejor práctica para evitar
        // que el hash cambie cuando se asigna un ID.
        return getClass().hashCode();
    }
}