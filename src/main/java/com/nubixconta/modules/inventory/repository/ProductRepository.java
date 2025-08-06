package com.nubixconta.modules.inventory.repository;

import com.nubixconta.modules.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer>{
    // =========================================================================================
    // == INICIO DE CÓDIGO MODIFICADO: Métodos "Tenant-Aware"
    // =========================================================================================
    // Busca un producto por su código DENTRO de una empresa específica.
    Optional<Product> findByCompany_IdAndProductCode(Integer companyId, String productCode);

    // CORREGIDO: Usando 'ProductName' para el ordenamiento.
    List<Product> findByCompany_IdOrderByProductNameAsc(Integer companyId);
    // Busca productos activos DENTRO de una empresa específica.
    List<Product> findByCompany_IdAndProductStatusTrueOrderByProductNameAsc(Integer companyId);

    // CORREGIDO: Usando 'ProductStatusFalse' y 'ProductName' para el ordenamiento.
    List<Product> findByCompany_IdAndProductStatusFalseOrderByProductNameAsc(Integer companyId);

    // Comprueba si un código de producto existe DENTRO de una empresa específica.
    boolean existsByCompany_IdAndProductCode(Integer companyId, String productCode);

    // Comprueba si un código de producto existe DENTRO de una empresa, excluyendo un ID de producto.
    boolean existsByCompany_IdAndProductCodeAndIdProductNot(Integer companyId, String productCode, Integer idProduct);

    // La búsqueda compleja ahora también requiere el companyId para el aislamiento.
    @Query(value = "SELECT * FROM product p " +
            "WHERE p.company_id = :companyId AND p.product_status = true " +
            "AND (:code IS NULL OR :code = '' OR p.product_code ILIKE :code) " +
            "AND (CAST(:name AS TEXT) IS NULL OR :name = '' OR p.product_name ILIKE :name)",
            nativeQuery = true)
    List<Product> searchActive(
            @Param("companyId") Integer companyId,
            @Param("code") String code,
            @Param("name") String name
    );
    // =========================================================================================
    // == FIN DE CÓDIGO MODIFICADO
    // =========================================================================================
}
