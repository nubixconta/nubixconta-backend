package com.nubixconta.modules.accountsreceivable.dto;

import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountsReceivableDTO {

    private BigDecimal balance;
    private Sale sale;
    private List<CollectionDetailDTO> collectionDetails;
    private Integer creditDay;

}

