package com.nubixconta.modules.accountsreceivable.entity;



import jakarta.persistence.*;
        import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class CollectionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
    private LocalDateTime date;
    private String identifier;

    @ManyToOne
    @JoinColumn(name = "collection_detail_id")
    private CollectionDetail collectionDetail;

    // Getters and setters
}
