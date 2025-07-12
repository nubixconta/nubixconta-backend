package com.nubixconta.modules.accountsreceivable.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collection_detail")
@Data
public class CollectionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collection_detail_id")
    private Integer id;


    @NotNull(message = "La cuenta por cobrar es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_receivable_id", nullable = false)
    @JsonBackReference
    private AccountsReceivable accountReceivable;

    @Column(name = "account_id")
    private Integer accountId;

    @NotNull(message = "La referencia es obligatoria")
    @Size(max = 30, message = "La referencia no puede tener mas de 30 caracteres")
    @Column(length = 30)
    private String reference;

    @NotNull(message = "El método de pago es obligatorio")
    @Size(max = 20, message = "El metodo no puede tener mas de 20 caracteres")
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @NotNull(message = "El estado del pago es obligatorio")
    @Size(max = 10, message = "El estado no puede tener más 10 caracteres")
    @Column(name = "payment_status", length = 10)
    private String paymentStatus;

    @NotNull(message = "El monto es obligatorio")
    @Column(name = "payment_amount", precision = 10, scale = 2)
    @Digits(integer = 8, fraction = 2, message = "El monto puede tener hasta 8 dígitos enteros y 2 decimales")
    @DecimalMin(value="0.00",inclusive=true,message = "El Monto no puede ser negativo")
    private BigDecimal paymentAmount;

    @NotNull(message = "La descripción es obligatoria")
    @Column(name = "payment_detail_description", length = 255)
    @Size(max = 255, message = "La descripción no puede tener más 255 caracteres")
    private String paymentDetailDescription;

    @NotNull(message = "La fecha es obligatorio")
    @Column(name = "collection_detail_date")
    private LocalDateTime CollectionDetailDate;

    @NotNull(message = "El módulo es obligatorio")
    @Size(max = 30, message = "La el modulo no puede tener mas de 30 caracteres")
    @Column(name = "module_type", length = 30)
    private String moduleType;

    @OneToMany(mappedBy = "collectionDetail", cascade = CascadeType.ALL)
    private List<CollectionEntry> collectionEntries;

    public void addEntry(CollectionEntry entry) {
        if (this.collectionEntries == null) {
            this.collectionEntries = new ArrayList<>();
        }
        this.collectionEntries.add(entry);
        entry.setCollectionDetail(this);
    }

    public void removeEntry(CollectionEntry entry) {
        if (this.collectionEntries != null) {
            this.collectionEntries.remove(entry);
            entry.setCollectionDetail(null);
        }
    }

}

