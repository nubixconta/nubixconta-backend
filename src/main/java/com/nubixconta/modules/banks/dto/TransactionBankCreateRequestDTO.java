// modules/banks/dto/TransactionBankCreateRequestDTO.java
package com.nubixconta.modules.banks.dto;

import com.nubixconta.modules.accounting.dto.BankEntryDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TransactionBankCreateRequestDTO {

    @NotNull(message = "La fecha de la transacción es obligatoria")
    private LocalDate transactionDate;

     private BigDecimal totalAmount;

    @NotBlank(message = "El tipo de transacción es obligatorio")
    @Size(max = 30, message = "El tipo de transacción puede tener máximo 30 caracteres")
    private String transactionType; // ENTRADA / SALIDA

    @NotBlank(message = "El número de referencia es obligatorio")
    @Size(max = 15, message = "El número de referencia puede tener máximo 15 caracteres")
    private String receiptNumber;

    @NotBlank(message = "La descripción general es obligatoria")
    @Size(max = 255, message = "La descripción general puede tener máximo 255 caracteres")
    private String description;

    // El estado y tipo de módulo se establecerán en el servicio
    // private String accountingTransactionStatus; // "PENDIENTE"
    // private String moduleType; // "BANCOS"

    // La empresa se obtendrá del contexto, no se recibirá directamente
    // private Integer companyId;

    @Valid // Para validar los elementos de la lista
    @NotNull(message = "Debe haber al menos un asiento contable")
    private List<BankEntryDTO> bankEntries;
}