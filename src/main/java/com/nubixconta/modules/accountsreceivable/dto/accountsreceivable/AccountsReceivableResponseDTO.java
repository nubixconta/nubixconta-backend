package com.nubixconta.modules.accountsreceivable.dto.accountsreceivable;

import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailResponseDTO;
import com.nubixconta.modules.sales.dto.sales.SaleForAccountsReceivableDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountsReceivableResponseDTO {

    private BigDecimal balance;
    private SaleForAccountsReceivableDTO sale;
    private List<CollectionDetailResponseDTO> collectionDetails;
    private Integer creditDay;

}

