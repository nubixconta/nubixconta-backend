package com.nubixconta.modules.accountsreceivable.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Entity
public class AccountsReceivable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer accountReceivableId;
    private BigDecimal amount;
    private  String receivableAccountStatus;
    private LocalDateTime receivableAccountDate;
    private String moduleType;

    private List<CollectionDetail> collectionDetails;
}
