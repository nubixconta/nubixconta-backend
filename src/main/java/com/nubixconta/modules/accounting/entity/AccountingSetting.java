package com.nubixconta.modules.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "accounting_setting")
@Getter
@Setter
public class AccountingSetting {

    @Id
    @Column(name = "setting_key", length = 50)
    private String settingKey;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}