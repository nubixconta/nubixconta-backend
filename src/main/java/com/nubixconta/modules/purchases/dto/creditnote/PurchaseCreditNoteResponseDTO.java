package com.nubixconta.modules.purchases.dto.creditnote;

import com.nubixconta.modules.purchases.dto.purchases.PurchaseSummaryDTO; // Asumimos que existe un DTO resumido
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseCreditNoteResponseDTO {
    private Integer idPurchaseCreditNote;
    private String documentNumber;
    private String description;
    private String creditNoteStatus;
    private BigDecimal subtotalAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private LocalDateTime issueDate;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
    private PurchaseSummaryDTO purchase; // Información resumida de la compra asociada
    private List<PurchaseCreditNoteDetailResponseDTO> details;
}