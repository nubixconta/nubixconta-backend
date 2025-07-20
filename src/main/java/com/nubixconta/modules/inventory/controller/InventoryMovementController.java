package com.nubixconta.modules.inventory.controller;

import com.nubixconta.modules.inventory.entity.InventoryMovement;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.service.InventoryMovementService;
import com.nubixconta.modules.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Field;
import org.springframework.util.ReflectionUtils;

@RestController
@RequestMapping("/api/v1/inventory-movements")
@RequiredArgsConstructor
public class InventoryMovementController {

    private final InventoryMovementService movementService;
    private final ProductService productService;

    @GetMapping
    public List<InventoryMovement> getAllMovements() {
        return movementService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryMovement> getMovement(@PathVariable Integer id) {
        return movementService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-product/{productId}")
    public List<InventoryMovement> getMovementsByProduct(@PathVariable Integer productId) {
        return movementService.findByProductId(productId);
    }

    @PostMapping
    public ResponseEntity<InventoryMovement> createMovement(@Valid @RequestBody Map<String, Object> body) {
        Integer idProduct = (Integer) body.get("idProduct");
        Optional<Product> optProduct = productService.findById(idProduct);
        if (optProduct.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(optProduct.get());
        movement.setDate(LocalDateTime.parse((String) body.get("date")));
        movement.setMovementType((String) body.get("movementType"));
        movement.setMovementDescription((String) body.get("movementDescription"));
        movement.setModule((String) body.get("module"));
        return ResponseEntity.ok(movementService.save(movement));
    }


    @PatchMapping("/{id}")
    public ResponseEntity<InventoryMovement> updateMovement(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> updates) {
        Optional<InventoryMovement> optionalMovement = movementService.findById(id);
        if (optionalMovement.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InventoryMovement movement = optionalMovement.get();

        if (updates.containsKey("date")) {
            try {
                movement.setDate(LocalDateTime.parse(updates.get("date").toString()));
            } catch (Exception e) {
                // Maneja error de formato de fecha si quieres
            }
        }
        if (updates.containsKey("movementType")) {
            movement.setMovementType(updates.get("movementType").toString());
        }
        if (updates.containsKey("movementDescription")) {
            movement.setMovementDescription(updates.get("movementDescription").toString());
        }
        if (updates.containsKey("module")) {
            movement.setModule(updates.get("module").toString());
        }
        if (updates.containsKey("idProduct")) {
            try {
                Integer idProduct = Integer.parseInt(updates.get("idProduct").toString());
                productService.findById(idProduct).ifPresent(movement::setProduct);
            } catch (Exception e) {
                // Maneja error de idProduct si quieres
            }
        }

        return ResponseEntity.ok(movementService.save(movement));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovement(@PathVariable Integer id) {
        try {
            movementService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}