package com.nubixconta.modules.inventory.repository;
import com.nubixconta.modules.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer>{
    Optional<Product> findByProductCode(String productCode);

}
