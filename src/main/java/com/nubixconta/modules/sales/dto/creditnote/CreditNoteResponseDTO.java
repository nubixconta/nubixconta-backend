package com.nubixconta.modules.sales.dto.creditnote;

import com.nubixconta.modules.sales.dto.sales.SaleSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
public class CreditNoteResponseDTO {
    private Integer idNotaCredit;
    private String documentNumber;
    private String description;
    private String creditNoteStatus;
    private BigDecimal totalAmount;
    // --- ¡AÑADIR ESTOS DOS CAMPOS NUEVOS! ---
    private BigDecimal subtotalAmount;
    private BigDecimal vatAmount;
    private LocalDateTime issueDate;    // La fecha de negocio.
    private LocalDateTime creationDate; // La fecha de auditoría.
    private LocalDateTime updateDate;
    private SaleSummaryDTO sale; // Información resumida de la venta asociada
    private List<CreditNoteDetailResponseDTO> details;
}