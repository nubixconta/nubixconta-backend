package com.nubixconta.modules.sales.repository;
import com.nubixconta.modules.sales.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Integer>{
    // Buscar clientes ACTIVOS por criterios flexibles
    @Query("SELECT c FROM Customer c WHERE c.status = true "
            + "AND (:name IS NULL OR LOWER(c.customerName) LIKE LOWER(CONCAT('%', :name, '%'))) "
            + "AND (:lastName IS NULL OR LOWER(c.customerLastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) "
            + "AND (:dui IS NULL OR c.customerDui = :dui) "
            + "AND (:nit IS NULL OR c.customerNit = :nit)")
    List<Customer> searchActive(
            @Param("name") String name,
            @Param("lastName") String lastName,
            @Param("dui") String dui,
            @Param("nit") String nit
    );

    // Buscar clientes DESACTIVADOS (sin filtro de búsqueda, solo lista)
    List<Customer> findByStatusFalse();
    List<Customer> findByStatusTrue();

}
