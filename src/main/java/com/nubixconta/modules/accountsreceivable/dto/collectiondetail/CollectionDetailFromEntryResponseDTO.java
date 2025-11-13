package com.nubixconta.modules.accountsreceivable.dto.collectiondetail;

import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
public class CollectionDetailFromEntryResponseDTO {
    private Integer id;
    private String moduleType;
    private String reference;
    public CollectionDetailFromEntryResponseDTO(){}

}
