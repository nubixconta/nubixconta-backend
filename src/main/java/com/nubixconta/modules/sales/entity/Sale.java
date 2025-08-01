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
import java.util.Set;
import java.util.HashSet;
@Entity
@Table(name="sale")
@Getter
@Setter
@NoArgsConstructor
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Integer saleId;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Customer customer;


    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    @Column(name = "document_number", length = 20, nullable = false,unique = true)
    private String documentNumber;

    @NotBlank(message = "El estado de la venta es obligatorio")
    @Size(max = 10, message = "El estado puede tener máximo 10 caracteres")
    @Column(name = "sale_status", length = 10, nullable = false)
    private String saleStatus;

    @NotNull(message = "La fecha de emisión es obligatoria")
    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;


    @NotNull(message = "El monto total es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El monto total debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @NotNull(message = "El subtotal (suma sin impuestos) es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El subtotal debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "subtotal_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotalAmount; // Almacena la suma de las líneas antes de impuestos

    @NotNull(message = "El monto de IVA es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El monto de IVA debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "vat_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal vatAmount; // Almacena el IVA calculado por el frontend

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción puede tener máximo 255 caracteres")
    @Column(name = "sale_description", length = 255, nullable = false)
    private String saleDescription;

    @NotBlank(message = "El módulo es obligatorio")
    @Size(max = 30, message = "El módulo puede tener máximo 30 caracteres")
    @Column(name = "module_type", length = 30, nullable = false)
    private String moduleType;


    // Relación con SaleDetail
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SaleDetail> saleDetails = new HashSet<>();

    @OneToMany( mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CreditNote> creditNotes = new HashSet<>();

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;


    public void addDetail(SaleDetail detail) {
        if (this.saleDetails == null) {
            this.saleDetails = new HashSet<>();
        }
        this.saleDetails.add(detail);
        detail.setSale(this); // <-- ¡Esta línea es la magia! Establece el lado inverso de la relación.
    }

    public void removeDetail(SaleDetail detail) {
        if (this.saleDetails != null) {
            this.saleDetails.remove(detail);
            detail.setSale(null); // Limpia la relación
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sale that)) return false;
        // Las ventas son únicas por su ID, si existe.
        return saleId != null && saleId.equals(that.saleId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
