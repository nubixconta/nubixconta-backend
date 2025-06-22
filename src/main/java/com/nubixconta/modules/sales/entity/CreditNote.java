package com.nubixconta.modules.sales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_note")
@Data
public class CreditNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_credit")
    private Integer idNotaCredit;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento puede tener máximo 20 caracteres")
    @Column(name = "document_number", length = 20, nullable = false)
    private String documentNumber;

    @NotBlank(message = "El estado de la nota de crédito es obligatorio")
    @Size(max = 10, message = "El estado puede tener máximo 10 caracteres")
    @Column(name = "credit_note_status", length = 10, nullable = false)
    private String creditNoteStatus;

    @NotNull(message = "La fecha de la nota de crédito es obligatoria")
    @Column(name = "credit_note_date", nullable = false)
    private LocalDateTime creditNoteDate;

    // Nueva relación: cada nota de crédito pertenece a UNA venta
    @NotNull(message = "La venta asociada es obligatoria")
    @ManyToOne(optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;
}
