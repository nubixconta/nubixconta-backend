package com.nubixconta.modules.inventory.repository;
import com.nubixconta.modules.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer>{
    Optional<Product> findByProductCode(String productCode);

    List<Product> findByProductStatusTrue();
    List<Product> findByProductStatusFalse();

    // Búsqueda solo en productos activos
    @Query("SELECT p FROM Product p WHERE p.productStatus = true "
            + "AND (:id IS NULL OR p.idProduct = :id) "
            + "AND (:code IS NULL OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :code, '%'))) "
            + "AND (:name IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Product> searchActive(
            @Param("id") Integer id,
            @Param("code") String code,
            @Param("name") String name
    );
}
