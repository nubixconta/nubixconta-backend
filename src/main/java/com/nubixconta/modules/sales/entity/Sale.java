package com.nubixconta.modules.sales.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="sale")
@Data
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Integer saleId;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "id_nota_credit")
    private CreditNote creditNote;

    @Column(name = "document_number", length = 20)
    private String documentNumber;

    @Column(name = "sale_status", length = 10)
    private String saleStatus;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "sale_type", length = 10)
    private String saleType;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "sale_date")
    private LocalDateTime saleDate;

    @Column(name = "sale_description", length = 255)
    private String saleDescription;

    @Column(name = "module_type", length = 30)
    private String moduleType;

    // Relación con SaleDetail
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleDetail> saleDetails;
}
