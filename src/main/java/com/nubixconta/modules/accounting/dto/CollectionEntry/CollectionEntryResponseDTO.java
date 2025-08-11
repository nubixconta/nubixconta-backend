package com.nubixconta.modules.accounting.dto.CollectionEntry;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//esta data se ocupa para ver el asiento contable en CXC
@Data
public class CollectionEntryResponseDTO {

    private Integer id;
    private String codAccount;
    private String documentNumber;
    private String custumerName;
    private String tipo;
    private String status;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
    private LocalDateTime date;

}
