package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.dto.CollectionEntry.CollectionEntryResponseDTO;
import com.nubixconta.modules.accounting.dto.PaymentEntry.PaymentEntryResponseDTO;
import com.nubixconta.modules.accounting.service.CollectionEntryService;
import com.nubixconta.modules.accounting.service.PaymentEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-entry")
public class PaymentEntryController {

    private final PaymentEntryService service;

    public PaymentEntryController(PaymentEntryService service) {
        this.service = service;
    }

    @GetMapping("/detail/{detailId}")
    public ResponseEntity<List<PaymentEntryResponseDTO>> getEntriesByDetailId(@PathVariable Integer detailId) {
        List<PaymentEntryResponseDTO> entries = service.getEntriesByDetailId(detailId);
        if (entries.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/from-detail/{detailId}")
    public ResponseEntity<Void> createFromDetail(@PathVariable Integer detailId) {
        service.ApplyPaymentDetail(detailId);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/by-detail/{detailId}")
    public ResponseEntity<Void> deleteByDetailId(@PathVariable Integer detailId) {
        service.cancelByDetailId(detailId);
        return ResponseEntity.noContent().build();
    }
}
