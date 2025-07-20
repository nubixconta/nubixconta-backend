package com.nubixconta.modules.accountsreceivable.controller;

import com.nubixconta.modules.accountsreceivable.service.CollectionEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collection-entry")
public class CollectionEntryController {

    private final CollectionEntryService service;

    public CollectionEntryController(CollectionEntryService service) {
        this.service = service;
    }

    @PostMapping("/from-detail/{detailId}")
    public ResponseEntity<Void> createFromDetail(@PathVariable Integer detailId) {
        service.createEntriesFromDetail(detailId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
