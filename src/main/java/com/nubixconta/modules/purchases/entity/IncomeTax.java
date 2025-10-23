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

/**
 * Entidad que representa la retención de Impuesto Sobre la Renta (ISR)
 * aplicada sobre una compra específica ya existente.
 */
@Entity
@Table(name = "income_tax", uniqueConstraints = {
        // El número de documento debe ser único por empresa
        @UniqueConstraint(columnNames = {"company_id", "document_number"})
})
@Getter
@Setter
@NoArgsConstructor
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public class IncomeTax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_income_tax")
    private Integer idIncomeTax;

    @NotNull(message = "La compra asociada es obligatoria")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_purchase", nullable = false)
    private Purchase purchase;

    @NotNull(message = "La empresa es obligatoria")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 100, message = "El número de documento puede tener máximo 100 caracteres")
    @Column(name = "document_number", length = 100, nullable = false)
    private String documentNumber;

    @NotBlank(message = "El estado de la retención es obligatorio")
    @Size(max = 10, message = "El estado puede tener máximo 10 caracteres")
    @Column(name = "income_tax_status", length = 10, nullable = false)
    private String incomeTaxStatus;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 256, message = "La descripción puede tener máximo 256 caracteres")
    @Column(name = "description", length = 256, nullable = false)
    private String description;

    @NotNull(message = "La fecha de emisión es obligatoria")
    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @NotNull(message = "El monto a aplicar es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "Formato de monto inválido")
    @Column(name = "amount_income_tax", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountIncomeTax;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    // --- equals() y hashCode() seguros para JPA ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IncomeTax that)) return false;
        return idIncomeTax != null && idIncomeTax.equals(that.idIncomeTax);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}