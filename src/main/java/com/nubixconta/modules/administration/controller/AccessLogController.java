package com.nubixconta.modules.administration.controller;
import com.nubixconta.modules.administration.dto.AccessLog.AccessLogResponseDTO;
import com.nubixconta.modules.administration.service.AccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/access-logs")
public class AccessLogController {

    @Autowired
    private AccessLogService accessLogService;

    @GetMapping
    public ResponseEntity<List<AccessLogResponseDTO>> getFilteredAccessLogs(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<AccessLogResponseDTO> accessLogs = accessLogService.getFilteredAccessLogs(userId, startDate, endDate);
        return ResponseEntity.ok(accessLogs);
    }
}