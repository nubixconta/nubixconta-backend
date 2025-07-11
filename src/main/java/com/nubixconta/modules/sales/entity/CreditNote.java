package com.nubixconta.modules.sales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @NotBlank(message = "El estado de la nota de crédito es obligatorio")
    @Size(max = 10, message = "El estado puede tener máximo 10 caracteres")
    @Column(name = "credit_note_status", length = 10, nullable = false)
    private String creditNoteStatus;

    @CreationTimestamp
    @Column(name = "credit_note_date", nullable = false, updatable = false)
    private LocalDateTime creditNoteDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @NotNull(message = "La venta asociada es obligatoria")
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false, unique = true) // 'unique = true' refuerza la regla
    private Sale sale;

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