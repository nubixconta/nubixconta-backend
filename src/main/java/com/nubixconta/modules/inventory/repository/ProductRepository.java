package com.nubixconta.modules.inventory.repository;
import com.nubixconta.modules.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String>{
}
