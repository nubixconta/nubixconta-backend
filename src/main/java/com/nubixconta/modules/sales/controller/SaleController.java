package com.nubixconta.modules.sales.controller;

import com.nubixconta.modules.inventory.service.ProductService;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.entity.SaleDetail;
import com.nubixconta.modules.sales.service.SaleService;
import com.nubixconta.modules.sales.service.SaleDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Field;
import org.springframework.util.ReflectionUtils;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final SaleDetailService saleDetailService;
    private final ProductService productService;

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
    public ResponseEntity<Sale> createSale(@Valid @RequestBody Sale sale) {
        return ResponseEntity.ok(saleService.save(sale));
    }


    // PATCH: actualización parcial y validación campo a campo
    @PatchMapping("/{id}")
    public ResponseEntity<Sale> patchSale(@PathVariable Integer id, @RequestBody Map<String, Object> updates) {
        Optional<Sale> optionalSale = saleService.findById(id);
        if (optionalSale.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Sale sale = optionalSale.get();

        updates.forEach((key, value) -> {
            if (key.equalsIgnoreCase("saleId")) {
                return; // No permitir cambiar la PK
            }
            Field field = ReflectionUtils.findField(Sale.class, key);
            if (field != null) {
                field.setAccessible(true);
                ReflectionUtils.setField(field, sale, value);
            }
        });

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

    @GetMapping("/details")
    public List<SaleDetail> getAllSaleDetails() {
        return saleDetailService.findAll();
    }

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
    public ResponseEntity<SaleDetail> createSaleDetail(@PathVariable Integer saleId, @Valid @RequestBody SaleDetail saleDetail) {
        return saleService.findById(saleId)
                .map(sale -> {
                    saleDetail.setSale(sale);
                    return ResponseEntity.ok(saleDetailService.save(saleDetail));
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @PatchMapping("/details/{detailId}")
    public ResponseEntity<SaleDetail> patchSaleDetail(
            @PathVariable Integer detailId,
            @RequestBody Map<String, Object> updates) {

        Optional<SaleDetail> optionalDetail = saleDetailService.findById(detailId);
        if (optionalDetail.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        SaleDetail saleDetail = optionalDetail.get();

        updates.forEach((key, value) -> {
            if (key.equalsIgnoreCase("saleDetailId")) {
                return; // No permitir cambiar la PK
            }
            if (key.equalsIgnoreCase("product")) {
                // Manejo especial para relación con Product
                Map<String, Object> productMap = (Map<String, Object>) value;
                Integer idProduct = Integer.parseInt(productMap.get("idProduct").toString());
                productService.findById(idProduct).ifPresent(saleDetail::setProduct);
                return;
            }
            Field field = ReflectionUtils.findField(SaleDetail.class, key);
            if (field != null) {
                field.setAccessible(true);
                // Conversión especial para BigDecimal
                if (field.getType().equals(BigDecimal.class)) {
                    if (value instanceof Number) {
                        ReflectionUtils.setField(field, saleDetail, BigDecimal.valueOf(((Number) value).doubleValue()));
                    } else {
                        ReflectionUtils.setField(field, saleDetail, new BigDecimal(value.toString()));
                    }
                } else {
                    ReflectionUtils.setField(field, saleDetail, value);
                }
            }
        });

        return ResponseEntity.ok(saleDetailService.save(saleDetail));
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
    // --- Buscar ventas por fechas ---
    @GetMapping("/search")
    public List<Sale> searchSalesByDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Debe enviar ambos parámetros: start y end.");
        }
        return saleService.findByIssueDateBetween(start, end);
    }


    // --- Buscar ventas por nombre/apellido cliente dui o nit---
    @GetMapping("/client-search")
    public List<Sale> searchSalesByCustomer(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String dui,
            @RequestParam(required = false) String nit
    ) {
        // Si no hay criterios, puedes devolver error o todas (según negocio)
        if ((name == null || name.isBlank()) &&
                (lastName == null || lastName.isBlank()) &&
                (dui == null || dui.isBlank()) &&
                (nit == null || nit.isBlank())
        ) {
            throw new IllegalArgumentException("Debe enviar al menos un criterio de búsqueda.");
        }
        return saleService.findByCustomerSearch(name, lastName, dui, nit);
    }
}
