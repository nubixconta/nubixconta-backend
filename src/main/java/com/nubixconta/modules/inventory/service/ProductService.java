package com.nubixconta.modules.inventory.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }
    public Product findById(String id) {
        return productRepository.findById(id).orElse(null);
    }
    public Product save(Product product) {
        return productRepository.save(product);
    }
    public void delete(String id) {
        productRepository.deleteById(id);
    }
}
