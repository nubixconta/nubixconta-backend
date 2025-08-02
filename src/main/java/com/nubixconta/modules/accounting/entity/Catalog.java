package com.nubixconta.modules.accounting.entity;

import com.nubixconta.modules.administration.entity.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Entidad de Enlace 'Catalog'.
 * Representa la asociación entre una cuenta maestra del plan de cuentas (Account)
 * y una empresa específica (Company).
 * Cada registro en esta tabla significa "La empresa X ha activado la cuenta Y para su uso".
 */
@Entity
@Table(name = "catalog", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "company_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Catalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_catalog")
    private Integer id;

    // Relación con la cuenta maestra. Muchas entradas de catálogo pueden apuntar a la misma cuenta.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // Relación con la empresa. Muchas entradas de catálogo pertenecerán a la misma empresa.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Este campo permite activar o desactivar el uso de una cuenta para una empresa
    // sin necesidad de borrar el registro, manteniendo la integridad histórica.
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    // Buenas prácticas de JPA para equals() y hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Catalog catalog = (Catalog) o;
        // La entidad es única por su ID, si ya ha sido persistida.
        return id != null && Objects.equals(id, catalog.id);
    }

    @Override
    public int hashCode() {
        // Usar el hash de la clase asegura consistencia antes de que la entidad sea persistida.
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Catalog{" +
                "id=" + id +
                ", accountId=" + (account != null ? account.getId() : "null") +
                ", companyId=" + (company != null ? company.getId() : "null") +
                ", activo=" + activo +
                '}';
    }
}