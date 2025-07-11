package com.nubixconta.modules.sales.dto.creditnote;

import com.nubixconta.modules.sales.dto.sales.SaleSummaryDTO;

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
    private String creditNoteStatus;
    private LocalDateTime creditNoteDate;
    private LocalDateTime updateDate;
    private SaleSummaryDTO sale; // Información resumida de la venta asociada
    private List<CreditNoteDetailResponseDTO> details;
}