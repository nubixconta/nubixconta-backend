package com.nubixconta.modules.inventory.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;

import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    // Trae todos los productos (activos e inactivos)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    // Solo activos
    public List<Product> findActive() {
        return productRepository.findByProductStatusTrue();
    }

    // Solo inactivos
    public List<Product> findInactive() {
        return productRepository.findByProductStatusFalse();
    }

    public Optional<Product> findById(Integer id) {
        return productRepository.findById(id);
    }

    public Optional<Product> findByProductCode(String code) {
        return productRepository.findByProductCode(code);
    }

    // Búsqueda flexible solo activos
    public List<Product> searchActive(Integer id, String code, String name) {
        return productRepository.searchActive(id,
                (code != null && !code.isBlank()) ? code : null,
                (name != null && !name.isBlank()) ? name : null
        );
    }


    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void delete(Integer id) {
        productRepository.deleteById(id);
    }
}
