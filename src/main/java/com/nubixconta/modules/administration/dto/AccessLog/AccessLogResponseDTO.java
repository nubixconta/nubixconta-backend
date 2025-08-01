package com.nubixconta.modules.administration.dto.AccessLog;

import com.nubixconta.modules.administration.dto.user.UserResponseNameDTO;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AccessLogResponseDTO {
    private Long id;
    private UserResponseNameDTO user;
    private LocalDate dateStartDate;
    private LocalTime dateStartTime;
    private LocalDate dateEndDate;
    private LocalTime dateEndTime;
}