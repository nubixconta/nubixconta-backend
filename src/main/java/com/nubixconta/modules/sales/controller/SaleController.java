package com.nubixconta.modules.sales.controller;

import com.nubixconta.modules.sales.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
public class SaleController {
    @Autowired
    private SaleService salesService;

    // TODO: los endpoint deben ir aqui
    /*
    @PostMapping
    public ResponseEntity<Sales> createSale(@RequestBody Sales sale) {
        return ResponseEntity.ok(salesService.save(sale));
    }

    // TODO: Endpoint de ejemplo para listar ventas
    @GetMapping
    public ResponseEntity<List<Sales>> getAllSales() {
        return ResponseEntity.ok(salesService.findAll());
    }

     */
}
