package com.nubixconta.modules.administration.dto.changehistory;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChangeHistoryResponseDTO {
    private String userFullName;
    private String companyName;
    private String moduleName;
    private LocalDateTime date;
    private String actionPerformed;
}
