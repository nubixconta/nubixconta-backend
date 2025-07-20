package com.nubixconta.modules.accountsreceivable.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
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
    @Column(length = 30)
    private String reference;

    @NotNull(message = "El método de pago es obligatorio")
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @NotNull(message = "El estado del pago es obligatorio")
    @Column(name = "payment_status", length = 10)
    private String paymentStatus;

    @NotNull(message = "El monto es obligatorio")
    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @NotNull(message = "La descripción es obligatoria")
    @Column(name = "payment_detail_description", length = 255)
    private String paymentDetailDescription;

    @NotNull(message = "El módulo es obligatorio")
    @Column(name = "module_type", length = 30)
    private String moduleType;

    @OneToMany(mappedBy = "collectionDetail", cascade = CascadeType.ALL)
    private List<CollectionEntry> collectionEntries;
}

