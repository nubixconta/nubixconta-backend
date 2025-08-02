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

    // Busca productos activos DENTRO de una empresa específica.
    List<Product> findByCompany_IdAndProductStatusTrue(Integer companyId);

    // Busca productos inactivos DENTRO de una empresa específica.
    List<Product> findByCompany_IdAndProductStatusFalse(Integer companyId);

    // Comprueba si un código de producto existe DENTRO de una empresa específica.
    boolean existsByCompany_IdAndProductCode(Integer companyId, String productCode);

    // Comprueba si un código de producto existe DENTRO de una empresa, excluyendo un ID de producto.
    boolean existsByCompany_IdAndProductCodeAndIdProductNot(Integer companyId, String productCode, Integer idProduct);

    // La búsqueda compleja ahora también requiere el companyId para el aislamiento.
    @Query("SELECT p FROM Product p WHERE p.company.id = :companyId AND p.productStatus = true "
            + "AND (:id IS NULL OR p.idProduct = :id) "
            + "AND (:code IS NULL OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :code, '%'))) "
            + "AND (:name IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Product> searchActive(
            @Param("companyId") Integer companyId,
            @Param("id") Integer id,
            @Param("code") String code,
            @Param("name") String name
    );
    // =========================================================================================
    // == FIN DE CÓDIGO MODIFICADO
    // =========================================================================================
}
