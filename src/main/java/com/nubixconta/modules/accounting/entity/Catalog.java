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

    // --- ¡NUEVA JERARQUÍA! ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_catalog_id")
    private Catalog parent;

    // --- ¡NUEVOS CAMPOS DE PERSONALIZACIÓN! ---
    @Column(name = "custom_name")
    private String customName;

    @Column(name = "custom_code")
    private String customCode;

    // Columna renombrada
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // --- LÓGICA DE NEGOCIO EN LA ENTIDAD (O EN UN DTO/SERVICE) ---
    // Estos métodos de conveniencia nos darán el valor "efectivo"
    @Transient
    public String getEffectiveName() {
        return this.customName != null ? this.customName : this.account.getAccountName();
    }

    @Transient
    public String getEffectiveCode() {
        return this.customCode != null ? this.customCode : this.account.getGeneratedCode();
    }

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
        // ESTA ES LA VERSIÓN SEGURA Y RECOMENDADA
        // Solo incluye el ID y los campos propios de esta entidad para evitar
        // errores de carga perezosa (LazyInitializationException).
        return "Catalog{" +
                "id=" + id +
                ", customName='" + customName + '\'' +
                ", customCode='" + customCode + '\'' +
                ", isActive=" + isActive +
                // No se deben incluir relaciones LAZY como account o company aquí.
                '}';
    }
}