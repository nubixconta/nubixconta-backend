package com.nubixconta.modules.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer id;

    // Relación de objeto que permite una navegación jerárquica eficiente.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;

    // Relación inversa para navegar de padre a hijos.
    @OneToMany(mappedBy = "parentAccount", fetch = FetchType.LAZY)
    private Set<Account> childAccounts = new HashSet<>();

    @Column(name = "account_name", length = 255, nullable = false)
    private String accountName;

    // Identificador único de negocio para búsquedas.
    @Column(name = "generated_code", nullable = false, unique = true)
    private String generatedCode;

    @Column(name = "account_type", length = 100, nullable = false)
    private String accountType;

    // Flag crucial para saber si la cuenta puede recibir movimientos.
    @Column(name = "is_postable", nullable = false)
    private boolean isPostable = false;

    // Métodos equals() y hashCode() seguros para JPA.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id != null && id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}