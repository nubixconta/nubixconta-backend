package com.nubixconta.modules.accountsreceivable.entity;

import com.nubixconta.modules.sales.entity.Sale;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private BigDecimal balance;

    @NotNull(message = "El estado es obligatorio")
    @Column(name = "receive_account_status", length = 10)
    private String receiveAccountStatus;

    @NotNull(message = "La fecha es obligatorio")
    @Column(name = "receivable_account_date")
    private LocalDateTime receivableAccountDate;

    @NotNull(message = "El modulo es obligatorio")
    @Column(name = "module_type", length = 30)
    private String moduleType;

    @OneToMany(mappedBy = "accountReceivable", cascade = CascadeType.ALL)
    private List<CollectionDetail> collectionDetails;


}
