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

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Integer id) {
        return productRepository.findById(id);
    }

    public Optional<Product> findByProductCode(String code) {
        return productRepository.findByProductCode(code);
    }


    public Product save(Product product) {
        return productRepository.save(product);
    }
    public void delete(Integer id) {
        productRepository.deleteById(id);
    }
}
