package com.nubixconta.modules.sales.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "credit_note")
@Data
public class CreditNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_credit")
    private Integer idNotaCredit;

    @Column(name = "document_number", length = 20)
    private String documentNumber;

    @Column(name = "credit_note_status", length = 10)
    private String creditNoteStatus;

    @Column(name = "credit_note_date")
    private LocalDateTime creditNoteDate;

    // Relación inversa: una nota de crédito puede estar asociada a varias ventas
    @OneToMany(mappedBy = "creditNote")
    @JsonIgnore // Evita ciclos JSON si serializas CreditNote con sus ventas
    private List<Sale> sales;
}
