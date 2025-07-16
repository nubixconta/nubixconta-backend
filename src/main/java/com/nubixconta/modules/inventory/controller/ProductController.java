package com.nubixconta.modules.inventory.controller;

import com.nubixconta.common.exception.BadRequestException;
import com.nubixconta.modules.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.Valid;
import com.nubixconta.modules.inventory.dto.product.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Obtener todos los productos (activos e inactivos)
    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        List<ProductResponseDTO> products = productService.findAll();
        if (products.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No hay productos registrados."));
        }
        return ResponseEntity.ok(products);
    }

    // Obtener solo productos activos
    @GetMapping("/active")
    public ResponseEntity<?> getActiveProducts() {
        List<ProductResponseDTO> products = productService.findActive();
        if (products.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No hay productos activos registrados."));
        }
        return ResponseEntity.ok(products);
    }

    // Obtener solo productos inactivos
    @GetMapping("/inactive")
    public ResponseEntity<?> getInactiveProducts() {
        List<ProductResponseDTO> products = productService.findInactive();
        if (products.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No hay productos inactivos registrados."));
        }
        return ResponseEntity.ok(products);
    }

    // Búsqueda flexible por ID, código o nombre
    @GetMapping("/search")
    public ResponseEntity<?> searchActiveProducts(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name
    ) {
        if ((id == null) && (code == null || code.isBlank()) && (name == null || name.isBlank())) {
            throw new BadRequestException("Debe enviar al menos un criterio de búsqueda.");
        }

        List<ProductResponseDTO> result = productService.searchActive(id, code, name);
        if (result.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No se encontraron productos con los criterios proporcionados."));
        }
        return ResponseEntity.ok(result);
    }

    //  Obtener un producto por ID
    @GetMapping("/{id}")
    public ProductResponseDTO getProduct(@PathVariable Integer id) {
        return productService.findById(id);
    }

    //  Obtener un producto por su código
    @GetMapping("/by-code/{code}")
    public ProductResponseDTO getProductByCode(@PathVariable String code) {
        return productService.findByProductCode(code);
    }

    // Crear nuevo producto
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductCreateDTO dto) {
        return ResponseEntity.ok(productService.create(dto));
    }

    //  Actualizar producto (excepto stock)
    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Integer id,
            @RequestBody ProductUpdateDTO dto) {
        return ResponseEntity.ok(productService.update(id, dto));
    }


    // Eliminar producto por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
