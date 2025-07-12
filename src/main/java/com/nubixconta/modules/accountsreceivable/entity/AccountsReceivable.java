package com.nubixconta.modules.accountsreceivable.entity;

import com.nubixconta.modules.sales.entity.Sale;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts_receivable")
@Data
public class AccountsReceivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_receivable_id")
    private Integer id;

    @NotNull(message = "La venta es obligatoria")
    @Column(name = "sale_id", nullable = false)
    private Integer saleId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sale_id", insertable = false, updatable = false)
    private Sale sale;

    @NotNull(message = "El saldo es obligatorio")
    @Column(precision = 10, scale = 2)
    @Digits(integer = 8, fraction = 2, message = "El saldo puede tener hasta 8 dígitos enteros y 2 decimales")
    @DecimalMin(value="0.00",inclusive=true,message = "El Saldo no puede ser negativo")
    private BigDecimal balance;


    @NotNull(message = "El modulo es obligatorio")
    @Size(max = 30, message = "La el modulo no puede tener mas de 30 caracteres")
    @Column(name = "module_type", length = 30)
    private String moduleType;

    @OneToMany(mappedBy = "accountReceivable", cascade = CascadeType.ALL)
    private List<CollectionDetail> collectionDetails;

    public void addCollectionDetail(CollectionDetail detail) {
        if (this.collectionDetails == null) {
            this.collectionDetails = new ArrayList<>();
        }
        this.collectionDetails.add(detail);
        detail.setAccountReceivable(this);
    }

    public void removeCollectionDetail(CollectionDetail detail) {
        if (this.collectionDetails != null) {
            this.collectionDetails.remove(detail);
            detail.setAccountReceivable(null);
        }
    }


}
