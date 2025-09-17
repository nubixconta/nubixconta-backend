package com.nubixconta.modules.purchases.entity;

import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.sales.entity.PersonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
// @FilterDef(...) // <-- LÍNEA ELIMINADA
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

    @NotBlank
    @Size(max = 100)
    @Column(name = "suplier_name", length = 100, nullable = false)
    private String supplierName;

    @Size(max = 100)
    @Column(name = "suplier_last_name", length = 100)
    private String supplierLastName;

    @Size(max = 10)
    @Column(name = "suplier_dui", length = 10)
    private String supplierDui;

    @Size(max = 17)
    @Column(name = "suplier_nit", length = 17)
    private String supplierNit;

    @NotBlank
    @Size(max = 14)
    @Column(name = "nrc", length = 14, nullable = false)
    private String nrc;

    @NotBlank
    @Size(max = 50)
    @Column(name = "address", length = 50, nullable = false)
    private String address;

    @NotBlank
    @Email
    @Size(max = 30)
    @Column(name = "email", length = 30, nullable = false)
    private String email;

    @NotBlank
    @Size(max = 8)
    @Column(name = "phone", length = 8, nullable = false)
    private String phone;

    @NotNull
    @Column(name = "credit_day", nullable = false)
    private Integer creditDay;

    @NotNull
    @Column(name = "credit_limit", precision = 10, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    @NotNull
    @Column(name = "current_balance", precision = 10, scale = 2, nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @NotNull
    @Column(name = "status", nullable = false)
    private Boolean status;

    @NotNull
    @Column(name = "exempt_from_vat", nullable = false)
    private Boolean exemptFromVat;

    @NotBlank
    @Size(max = 100)
    @Column(name = "business_activity", length = 100, nullable = false)
    private String businessActivity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", length = 30, nullable = false)
    private PersonType personType;

    @NotNull
    @Column(name = "applies_withholding", nullable = false)
    private Boolean appliesWithholding;

    @Size(max = 20)
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