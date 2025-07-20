package com.nubixconta.modules.accounting.controller;
import com.nubixconta.modules.accounting.service.CollectionEntryService;
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
        service.ApplyCollectionDetail(detailId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/by-detail/{detailId}")
    public ResponseEntity<Void> deleteByDetailId(@PathVariable Integer detailId) {
        service.cancelByDetailId(detailId);
        return ResponseEntity.noContent().build();
    }
}
