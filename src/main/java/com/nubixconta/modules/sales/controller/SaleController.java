package com.nubixconta.modules.sales.controller;

import com.nubixconta.modules.sales.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.entity.SaleDetail;
import com.nubixconta.modules.sales.service.SaleService;
import com.nubixconta.modules.sales.service.SaleDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final SaleDetailService saleDetailService;

    // --- Sale endpoints ---

    @GetMapping
    public List<Sale> getAllSales() {
        return saleService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSale(@PathVariable Integer id) {
        return saleService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Sale> createSale(@RequestBody Sale sale) {
        return ResponseEntity.ok(saleService.save(sale));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sale> updateSale(@PathVariable Integer id, @RequestBody Sale sale) {
        if (saleService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        sale.setSaleId(id);
        return ResponseEntity.ok(saleService.save(sale));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Integer id) {
        try {
            saleService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- SaleDetail endpoints ---

    @GetMapping("/{saleId}/details")
    public ResponseEntity<List<SaleDetail>> getSaleDetailsBySale(@PathVariable Integer saleId) {
        return saleService.findById(saleId)
                .map(sale -> ResponseEntity.ok(sale.getSaleDetails()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/details/{detailId}")
    public ResponseEntity<SaleDetail> getSaleDetail(@PathVariable Integer detailId) {
        return saleDetailService.findById(detailId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{saleId}/details")
    public ResponseEntity<SaleDetail> createSaleDetail(@PathVariable Integer saleId, @RequestBody SaleDetail saleDetail) {
        return saleService.findById(saleId)
                .map(sale -> {
                    saleDetail.setSale(sale);
                    return ResponseEntity.ok(saleDetailService.save(saleDetail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/details/{detailId}")
    public ResponseEntity<SaleDetail> updateSaleDetail(@PathVariable Integer detailId, @RequestBody SaleDetail saleDetail) {
        return saleDetailService.findById(detailId)
                .map(existingDetail -> {
                    saleDetail.setSaleDetailId(detailId);
                    saleDetail.setSale(existingDetail.getSale());
                    return ResponseEntity.ok(saleDetailService.save(saleDetail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/details/{detailId}")
    public ResponseEntity<Void> deleteSaleDetail(@PathVariable Integer detailId) {
        try {
            saleDetailService.delete(detailId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}