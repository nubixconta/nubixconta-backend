package com.nubixconta.modules.accounting.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "account")
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer id;

    @Column(name = "parent_account_id")
    private Integer parentAccountId;

    @Column(name = "account_name", length = 255, nullable = false)
    private String accountName;

    @Column(name = "individual_code", length = 30, nullable = false)
    private String individualCode;

    @Column(name = "account_type", length = 50, nullable = false)
    private String accountType;

    @Column(name = "generated_code", nullable = false)
    private String generatedCode;
}
