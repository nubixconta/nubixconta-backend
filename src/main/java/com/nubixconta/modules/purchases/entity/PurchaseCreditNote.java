package com.nubixconta.modules.purchases.entity;

import com.nubixconta.modules.administration.entity.Company;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "purchase_credit_note", uniqueConstraints = {
        // El número de documento debe ser único dentro de la misma empresa.
        @UniqueConstraint(columnNames = {"company_id", "document_number"})
})
@Getter
@Setter
@NoArgsConstructor
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public class PurchaseCreditNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_credit_note") // Corresponde a id_credit_note en el ERD
    private Integer id;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    @Column(name = "document_number", length = 20, nullable = false)
    private String documentNumber;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción puede tener máximo 255 caracteres")
    @Column(name = "description", length = 255)
    private String description;

    @NotBlank(message = "El estado de la nota de crédito es obligatorio")
    @Size(max = 10, message = "El estado puede tener máximo 10 caracteres")
    @Column(name = "credit_note_status", length = 10, nullable = false)
    private String creditNoteStatus;

    // Cambiado de credit_note_date a issueDate para consistencia con ventas
    @NotNull(message = "La fecha de emisión es obligatoria")
    @Column(name = "credit_note_date", nullable = false)
    private LocalDateTime issueDate;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @NotNull(message = "La compra asociada es obligatoria")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_purchase", nullable = false) // Corresponde a id_purchase en el ERD
    private Purchase purchase;

    @NotNull(message = "La empresa es obligatoria")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

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

    @OneToMany(
            mappedBy = "purchaseCreditNote",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<PurchaseCreditNoteDetail> details = new HashSet<>();

    // --- Métodos Helper ---
    public void addDetail(PurchaseCreditNoteDetail detail) {
        if (this.details == null) {
            this.details = new HashSet<>();
        }
        this.details.add(detail);
        detail.setPurchaseCreditNote(this); // Sincroniza el lado inverso
    }

    public void removeDetail(PurchaseCreditNoteDetail detail) {
        if (this.details != null) {
            this.details.remove(detail);
            detail.setPurchaseCreditNote(null);
        }
    }

    // --- equals() y hashCode() seguros para JPA ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseCreditNote that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}