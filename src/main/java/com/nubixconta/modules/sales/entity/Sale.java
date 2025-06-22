package com.nubixconta.modules.sales.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnoreProperties({
            "customerLastName",
            "customerDui",
            "customerNit",
            "ncr",
            "address",
            "email",
            "phone",
            "creditDay",
            "creditLimit",
            "status",
            "creationDate",
            "exemptFromVat",
            "businessActivity",
            "personType",
            "appliesWithholding"
    })
    private Customer customer;


    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    @Column(name = "document_number", length = 20, nullable = false)
    private String documentNumber;

    @NotBlank(message = "El estado de la venta es obligatorio")
    @Size(max = 10, message = "El estado puede tener máximo 10 caracteres")
    @Column(name = "sale_status", length = 10, nullable = false)
    private String saleStatus;

    @NotNull(message = "La fecha de emisión es obligatoria")
    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @NotBlank(message = "El tipo de venta es obligatorio")
    @Size(max = 10, message = "El tipo puede tener máximo 10 caracteres")
    @Column(name = "sale_type", length = 10, nullable = false)
    private String saleType;

    @NotNull(message = "El monto total es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "El monto total debe tener hasta 10 dígitos y 2 decimales")
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @NotNull(message = "La fecha de venta es obligatoria")
    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción puede tener máximo 255 caracteres")
    @Column(name = "sale_description", length = 255, nullable = false)
    private String saleDescription;

    @NotBlank(message = "El módulo es obligatorio")
    @Size(max = 30, message = "El módulo puede tener máximo 30 caracteres")
    @Column(name = "module_type", length = 30, nullable = false)
    private String moduleType;

    // Relación con SaleDetail
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({
            "sale",
            "product",
            "serviceName",
            "quantity",
            "unitPrice",
            "subtotal"

    })
    private List<SaleDetail> saleDetails;
    @OneToMany(mappedBy = "sale")
    @JsonIgnore
    private List<CreditNote> creditNotes;

}
