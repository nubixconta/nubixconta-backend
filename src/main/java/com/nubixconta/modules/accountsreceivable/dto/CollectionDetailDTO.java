package com.nubixconta.modules.accountsreceivable.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
@AllArgsConstructor
@Data
public class CollectionDetailDTO {
        private String paymentStatus;
        private String paymentDetailDescription;
        private LocalDateTime CollectionDetailDate;
        private String paymentMethod;

        //Se crea este constructor vacio por que ModelMapper necesita instanciar CollectionDetailDTO
       //Pero tiene que ser con el constructor vacio
        public CollectionDetailDTO(){}

}
