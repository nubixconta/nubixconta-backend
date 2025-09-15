package com.nubixconta.modules.accounting.dto.Account;

import lombok.Data;

@Data
public class AccountBankResponseDTO {

        private Integer id;
        private String accountName;
        private String generatedCode;
        private String accountType;

}
