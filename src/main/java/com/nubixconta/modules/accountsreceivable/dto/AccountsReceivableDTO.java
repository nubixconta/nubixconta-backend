package com.nubixconta.modules.accountsreceivable.dto;

import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AccountsReceivableDTO {

    private BigDecimal balance;
    private String receiveAccountStatus;
    private LocalDateTime receivableAccountDate;

    // incluir la venta completa (sin transformar) de momento
    private Sale sale;

    // incluir los detalles de cobro completos (sin transformar) de momento
    private List<CollectionDetail> collectionDetails;
    private Integer creditDay;

}

