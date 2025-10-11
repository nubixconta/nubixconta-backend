package com.nubixconta.modules.sales.entity;


import com.nubixconta.modules.administration.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.nubixconta.modules.administration.entity.Company;
import org.hibernate.annotations.Filter; // <-- NUEVO IMPORT
import org.hibernate.annotations.FilterDef; // <-- NUEVO IMPORT
import org.hibernate.annotations.ParamDef; // <-- NUEVO IMPORT
import java.math.BigDecimal;
import java.time.LocalDateTime;


// Se mueven las restricciones de unicidad aquí para que sean compuestas con el company_id.
// Esto permite que diferentes empresas tengan un cliente con el mismo NIT/DUI/NCR.
@Table(name = "customer", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"company_id", "customer_dui"}),
        @UniqueConstraint(columnNames = {"company_id", "customer_nit"}),
        @UniqueConstraint(columnNames = {"company_id", "ncr"})
})
@Entity
@Getter
@Setter
@NoArgsConstructor

// 2. Se aplica dicho filtro a esta entidad. Hibernate añadirá automáticamente la condición
//    "company_id = ?" a todas las consultas sobre esta entidad si el filtro está activo.
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id")
    private Integer clientId;

    // SE AÑADE LA RELACIÓN CON Company.
    // Esta es la relación clave para el aislamiento de datos.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre puede tener máximo 50 caracteres")
    @Column(name = "customer_name", length = 50, nullable = false)
    private String customerName;


    @Size(max = 50, message = "El apellido puede tener máximo 50 caracteres")
    @Column(name = "customer_last_name", length = 50)
    private String customerLastName;


    @Size(max = 10)
    @Column(name = "customer_dui", length = 10,unique = true)
    private String customerDui;

    @Size(max = 17)
    @Column(name = "customer_nit", length = 17,unique = true)
    private String customerNit;

    @NotBlank(message = "El NCR es obligatorio") // <-- ¡AÑADIR @NotBlank!
    @Size(max = 14, message = "El NCR puede tener máximo 14 caracteres")
    @Column(name = "ncr",nullable = false, length = 14,unique = true) // <-- AÑADIR UNIQUE
    private String ncr;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 50, message = "La dirección puede tener máximo 50 caracteres")
    @Column(name = "address", length = 50, nullable = false)
    private String address;

    @NotBlank(message = "El email es obligatorio")
    @Size(max = 30, message = "El email puede tener máximo 30 caracteres")
    @Email(message = "El email debe tener un formato válido")
    @Column(name = "email", length = 30, nullable = false,unique = true)
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 8, message = "El teléfono puede tener máximo 8 caracteres")
    @Column(name = "phone", length = 8, nullable = false,unique = true)
    private String phone;

    @NotNull(message = "El número de días de crédito es obligatorio")
    @Min(value = 0, message = "El número de días de crédito no puede ser negativo")
    @Column(name = "credit_day", nullable = false)
    private Integer creditDay;

    @NotNull(message = "El límite de crédito es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El límite de crédito debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "credit_limit", precision = 10, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    // Este campo llevará el control del saldo pendiente del cliente.
    // Lo inicializamos en CERO para nuevos clientes.
    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "current_balance", precision = 10, scale = 2, nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;


    @NotNull(message = "El estado es obligatorio")
    @Column(name = "status", nullable = false)
    private Boolean status;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;


    @NotNull(message = "El campo de exención de IVA es obligatorio")
    @Column(name = "exempt_from_vat", nullable = false)
    private Boolean exemptFromVat;

    @NotBlank(message = "La actividad económica es obligatoria")
    @Size(max = 100, message = "La actividad económica puede tener máximo 100 caracteres")
    @Column(name = "business_activity", length = 100, nullable = false)
    private String businessActivity;

    @NotNull(message = "El tipo de persona es obligatorio")
    @Enumerated(EnumType.STRING) // Le dice a JPA que guarde el nombre del enum ("NATURAL", "JURIDICA") en la BD
    @Column(name = "person_type", length = 30, nullable = false)
    private PersonType personType;

    @NotNull(message = "El campo de retención es obligatorio")
    @Column(name = "applies_withholding", nullable = false)
    private Boolean appliesWithholding;

    /**
     * Devuelve el nombre completo del cliente.
     * Concatena el nombre y el apellido si este último existe.
     * @return El nombre completo como una cadena de texto.
     */
    @Transient // Anotación importante: le dice a JPA que este método no es una columna en la BD.
    public String getFullName() {
        if (this.customerLastName != null && !this.customerLastName.isBlank()) {
            return this.customerName + " " + this.customerLastName;
        }
        return this.customerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        // Para entidades con ID, una vez que el ID no es nulo, es la única verdad.
        return clientId != null && clientId.equals(customer.clientId);
    }

    @Override
    public int hashCode() {
        // Usar una constante para objetos transitorios (sin ID)
        // y el hash del ID para los persistidos.
        return getClass().hashCode();
    }
}
