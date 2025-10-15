package com.nubixconta.modules.purchases.entity;

import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.sales.entity.PersonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
// import org.hibernate.annotations.FilterDef; // <-- LÍNEA ELIMINADA
// import org.hibernate.annotations.ParamDef; // <-- LÍNEA ELIMINADA
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "supplier", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"company_id", "suplier_dui"}),
        @UniqueConstraint(columnNames = {"company_id", "suplier_nit"}),
        @UniqueConstraint(columnNames = {"company_id", "nrc"})
})
@Getter
@Setter
@NoArgsConstructor
@Filter(name = "tenantFilter", condition = "company_id = :companyId") // <-- ESTA LÍNEA ES LA CORRECTA Y SE MANTIENE
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_supplier")
    private Integer idSupplier;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 100)
    @Column(name = "suplier_name", length = 100, nullable = false)
    private String supplierName;

    @Size(max = 100, message = "El apellido puede tener máximo 100 caracteres")
    @Column(name = "suplier_last_name", length = 100)
    private String supplierLastName;

    @Size(max = 10, message = "El DUI puede tener máximo 10 caracteres")
    @Column(name = "suplier_dui", length = 10)
    private String supplierDui;

    @Size(max = 17, message = "El NIT puede tener máximo 17 caracteres")
    @Column(name = "suplier_nit", length = 17)
    private String supplierNit;

    @NotBlank(message = "El NRC es obligatorio")
    @Size(max = 14, message = "El NRC puede tener máximo 14 caracteres")
    @Column(name = "nrc", length = 14, nullable = false)
    private String nrc;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 50, message = "La dirección puede tener máximo 50 caracteres")
    @Column(name = "address", length = 50, nullable = false)
    private String address;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 30, message = "El email puede tener máximo 30 caracteres")
    @Column(name = "email", length = 30, nullable = false)
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 8, message = "El teléfono puede tener máximo 8 caracteres")
    @Column(name = "phone", length = 8, nullable = false)
    private String phone;

    @NotNull(message = "El número de días de crédito es obligatorio")
    @Min(value = 0, message = "Los días de crédito no pueden ser negativos")
    @Column(name = "credit_day", nullable = false)
    private Integer creditDay;

    @NotNull(message = "El límite de crédito es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "Formato de límite de crédito inválido")
    @Column(name = "credit_limit", precision = 10, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    @NotNull
    @Column(name = "current_balance", precision = 10, scale = 2, nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @NotNull(message = "El estado es obligatorio")
    @Column(name = "status", nullable = false)
    private Boolean status;

    @NotNull(message = "El campo de exención de IVA es obligatorio")
    @Column(name = "exempt_from_vat", nullable = false)
    private Boolean exemptFromVat;

    @NotBlank(message = "La actividad económica es obligatoria")
    @Size(max = 100, message = "La actividad económica puede tener máximo 100 caracteres")
    @Column(name = "business_activity", length = 100, nullable = false)
    private String businessActivity;

    @NotNull(message = "El tipo de persona es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", length = 30, nullable = false)
    private PersonType personType;

    @NotNull(message = "El campo 'aplica percepción' es obligatorio")
    @Column(name = "applies_perception", nullable = false) // Columna en BD
    private Boolean appliesPerception;

    @Size(max = 20, message = "El tipo de proveedor puede tener máximo 20 caracteres")
    @Column(name = "suplier_type", length = 20)
    private String supplierType;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Supplier supplier = (Supplier) o;
        return idSupplier != null && idSupplier.equals(supplier.idSupplier);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}