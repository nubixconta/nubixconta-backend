package com.nubixconta.modules.purchases.repository;

import com.nubixconta.modules.purchases.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    // Búsquedas básicas por estado dentro del tenant
    List<Supplier> findByCompany_IdAndStatusTrueOrderByCreationDateDesc(Integer companyId);
    List<Supplier> findByCompany_IdAndStatusFalseOrderByCreationDateDesc(Integer companyId);

    // --- VALIDACIONES DE UNICIDAD A NIVEL DE TENANT ---
    boolean existsByCompany_IdAndSupplierDui(Integer companyId, String dui);
    boolean existsByCompany_IdAndSupplierDuiAndIdSupplierNot(Integer companyId, String dui, Integer idSupplier);

    boolean existsByCompany_IdAndSupplierNit(Integer companyId, String nit);
    boolean existsByCompany_IdAndSupplierNitAndIdSupplierNot(Integer companyId, String nit, Integer idSupplier);

    boolean existsByCompany_IdAndNrc(Integer companyId, String nrc);
    boolean existsByCompany_IdAndNrcAndIdSupplierNot(Integer companyId, String nrc, Integer idSupplier);

    // --- VALIDACIONES DE UNICIDAD A NIVEL GLOBAL (IGNORAN EL TENANT) ---
    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.supplierDui = :dui")
    boolean existsByDuiGlobal(@Param("dui") String dui);
    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.supplierDui = :dui AND s.idSupplier != :idSupplier")
    boolean existsByDuiGlobalAndIdSupplierNot(@Param("dui") String dui, @Param("idSupplier") Integer idSupplier);

    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.supplierNit = :nit")
    boolean existsByNitGlobal(@Param("nit") String nit);
    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.supplierNit = :nit AND s.idSupplier != :idSupplier")
    boolean existsByNitGlobalAndIdSupplierNot(@Param("nit") String nit, @Param("idSupplier") Integer idSupplier);

    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.nrc = :nrc")
    boolean existsByNrcGlobal(@Param("nrc") String nrc);
    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.nrc = :nrc AND s.idSupplier != :idSupplier")
    boolean existsByNcrGlobalAndIdSupplierNot(@Param("nrc") String nrc, @Param("idSupplier") Integer idSupplier);

    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.email = :email")
    boolean existsByEmailGlobal(@Param("email") String email);
    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.email = :email AND s.idSupplier != :idSupplier")
    boolean existsByEmailGlobalAndIdSupplierNot(@Param("email") String email, @Param("idSupplier") Integer idSupplier);

    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.phone = :phone")
    boolean existsByPhoneGlobal(@Param("phone") String phone);
    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.phone = :phone AND s.idSupplier != :idSupplier")
    boolean existsByPhoneGlobalAndIdSupplierNot(@Param("phone") String phone, @Param("idSupplier") Integer idSupplier);

    // --- MÉTODO DE BÚSQUEDA AÑADIDO ---
    // Busca proveedores activos combinando múltiples criterios opcionales.
    @Query("SELECT s FROM Supplier s WHERE s.company.id = :companyId AND s.status = true " +
            "AND (:name IS NULL OR LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:lastName IS NULL OR LOWER(s.supplierLastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) " +
            "AND (:dui IS NULL OR s.supplierDui = :dui) " +
            "AND (:nit IS NULL OR s.supplierNit = :nit)")
    List<Supplier> searchActive(
            @Param("companyId") Integer companyId,
            @Param("name") String name,
            @Param("lastName") String lastName,
            @Param("dui") String dui,
            @Param("nit") String nit
    );
}