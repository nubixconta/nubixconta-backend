package com.nubixconta.modules.accountsreceivable.dto.collectiondetail;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@AllArgsConstructor
@Data
public class CollectionDetailResponseDTO {
        private Integer id;
        private String paymentStatus;
        private String paymentDetailDescription;
        private LocalDateTime CollectionDetailDate;
        private String paymentMethod;
        private BigDecimal paymentAmount;
        private String reference;
        //Se crea este constructor vacio por que ModelMapper necesita instanciar CollectionDetailDTO
       //Pero tiene que ser con el constructor vacio
        public CollectionDetailResponseDTO(){}

}
