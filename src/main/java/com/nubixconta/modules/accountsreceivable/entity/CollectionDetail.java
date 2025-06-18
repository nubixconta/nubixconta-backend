package com.nubixconta.modules.accountsreceivable.entity;


import jakarta.persistence.*;
        import java.math.BigDecimal;
import java.util.List;

@Entity
public class CollectionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String reference;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal paymentAmount;
    private String paymentDetailDescription;
    private String moduleType;

    @ManyToOne
    @JoinColumn(name = "account_receivable_id")
    private AccountsReceivable accountReceivable;

    @OneToMany(mappedBy = "collectionDetail")
    private List<CollectionEntry> collectionEntries;


}

