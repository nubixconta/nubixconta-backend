package com.nubixconta.modules.accountsreceivable.entity;

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

    @ManyToOne
    @JoinColumn(name = "account_receivable_id", nullable = false)
    private AccountsReceivable accountReceivable;

    @Column(name = "account_id")
    private Integer accountId;

    @NotNull(message = "La referencia es obligatoria")
    @Column(length = 30)
    private String reference;

    @NotNull(message = "El metodo de pago es obligatorio")
    @Column(name = "payment_method", length = 10)
    private String paymentMethod;

    @NotNull(message = "El estado del pago es obligatorio")
    @Column(name = "payment_status", length = 10)
    private String paymentStatus;

    @NotNull(message = "El monto es obligatorio")
    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @NotNull(message = "La descripcion es obligatoria")
    @Column(name = "payment_detail_description", length = 255)
    private String paymentDetailDescription;

    @NotNull(message = "El modulo es obligatorio")
    @Column(name = "module_type", length = 30)
    private String moduleType;

    @OneToMany(mappedBy = "collectionDetail", cascade = CascadeType.ALL)
    private List<CollectionEntry> collectionEntries;

    // Getters y setters...
}


