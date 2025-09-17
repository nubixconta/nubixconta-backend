package com.nubixconta.modules.purchases.entity;

import com.nubixconta.modules.administration.entity.Company;
import jakarta.persistence.*;
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
@Table(name = "purchase", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"company_id", "document_number"})
})
@Getter
@Setter
@NoArgsConstructor
@Filter(name = "tenantFilter", condition = "company_id = :companyId") // <-- CORRECTO
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_purchase")
    private Integer idPurchase;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_supplier", nullable = false)
    private Supplier supplier;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank
    @Size(max = 20)
    @Column(name = "document_number", length = 20, nullable = false)
    private String documentNumber;

    @NotBlank
    @Size(max = 10)
    @Column(name = "purchase_status", length = 10, nullable = false)
    private String purchaseStatus;

    @NotNull
    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @NotNull
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @NotNull
    @Column(name = "subtotal_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotalAmount;

    @NotNull
    @Column(name = "vat_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal vatAmount;

    @NotBlank
    @Size(max = 255)
    @Column(name = "purchase_description", length = 255, nullable = false)
    private String purchaseDescription;

    @NotBlank
    @Size(max = 30)
    @Column(name = "module_type", length = 30, nullable = false)
    private String moduleType;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PurchaseDetail> purchaseDetails = new HashSet<>();

    public void addDetail(PurchaseDetail detail) {
        this.purchaseDetails.add(detail);
        detail.setPurchase(this);
    }

    public void removeDetail(PurchaseDetail detail) {
        this.purchaseDetails.remove(detail);
        detail.setPurchase(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Purchase purchase = (Purchase) o;
        return idPurchase != null && idPurchase.equals(purchase.idPurchase);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}