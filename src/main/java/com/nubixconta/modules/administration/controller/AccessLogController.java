package com.nubixconta.modules.administration.controller;
import com.nubixconta.modules.administration.dto.AccessLog.AccessLogResponseDTO;
import com.nubixconta.modules.administration.service.AccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/access-logs")
public class AccessLogController {

    @Autowired
    private AccessLogService accessLogService;

    @GetMapping
    public ResponseEntity<List<AccessLogResponseDTO>> getAllAccessLogs() {
        List<AccessLogResponseDTO> accessLogs = accessLogService.getAllAccessLogs();
        return ResponseEntity.ok(accessLogs);
    }
}