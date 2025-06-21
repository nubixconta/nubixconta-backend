package com.nubixconta.modules.inventory.controller;


import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Field;
import org.springframework.util.ReflectionUtils;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Integer id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint adicional para buscar por código de producto
    @GetMapping("/by-code/{code}")
    public ResponseEntity<Product> getProductByCode(@PathVariable String code) {
        return productService.findByProductCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        product.setIdProduct(null); // Garantiza que la PK no venga en el request
        return ResponseEntity.ok(productService.save(product));
    }


    @PatchMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> updates) {

        Optional<Product> optionalProduct = productService.findById(id);
        if (optionalProduct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product product = optionalProduct.get();

        updates.forEach((key, value) -> {
            if (key.equalsIgnoreCase("idProduct")) {
                return; // No permitir cambiar la PK
            }
            Field field = ReflectionUtils.findField(Product.class, key);
            if (field != null) {
                field.setAccessible(true);
                ReflectionUtils.setField(field, product, value);
            }
        });

        return ResponseEntity.ok(productService.save(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        try {
            productService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
