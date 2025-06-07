package com.nubixconta.modules.sales;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {
    @Autowired
    private SalesService salesService;

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
