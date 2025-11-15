package com.nubixconta.modules.accounting.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que mapea la vista de base de datos 'v_general_ledger'.
 * Es de solo lectura, lo que se indica con la anotación @Immutable.
 * Hibernate no intentará hacer updates o inserts sobre esta entidad.
 */
@Entity
@Immutable // <-- CRÍTICA: Le dice a JPA que es de solo lectura.
@Table(name = "v_general_ledger") // <-- CRÍTICA: Conecta esta clase con tu vista.
@Getter
public class GeneralLedgerView {

    @Id
    private String uniqueId; // Mapea a la columna 'unique_id'

    private Long documentId;
    private String documentType;
    private LocalDateTime accountingDate;
    private Integer idCatalog;
    private BigDecimal debe;
    private BigDecimal haber;
    private String description;
    private Integer companyId;

}