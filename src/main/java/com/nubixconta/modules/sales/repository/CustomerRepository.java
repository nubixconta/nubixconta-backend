package com.nubixconta.modules.sales.repository;
import com.nubixconta.modules.sales.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Integer>{
    /**
     * MODIFICADO: La consulta ahora requiere el companyId para asegurar el aislamiento de datos.
     * Aunque el filtro de Hibernate podría cubrirlo, ser explícito en consultas complejas es más seguro.
     */
    @Query("SELECT c FROM Customer c WHERE c.company.id = :companyId AND c.status = true "
            + "AND ( :name IS NULL OR :name = '' OR LOWER(c.customerName) LIKE LOWER(CONCAT('%', :name, '%')) ) "
            + "AND ( :lastName IS NULL OR :lastName = '' OR LOWER(c.customerLastName) LIKE LOWER(CONCAT('%', :lastName, '%')) ) "
            + "AND ( :dui IS NULL OR :dui = '' OR c.customerDui = :dui ) "
            + "AND ( :nit IS NULL OR :nit = '' OR c.customerNit = :nit )"
            + "ORDER BY c.creationDate DESC")
    List<Customer> searchActive(
            @Param("companyId") Integer companyId, // <-- NUEVO PARÁMETRO
            @Param("name") String name,
            @Param("lastName") String lastName,
            @Param("dui") String dui,
            @Param("nit") String nit
    );

    // MODIFICADO: La búsqueda de inactivos ahora también debe estar acotada a la empresa.
    List<Customer> findByCompany_IdAndStatusFalse(Integer companyId);

    // MODIFICADO: La búsqueda de activos ahora también debe estar acotada a la empresa.
    List<Customer> findByCompany_IdAndStatusTrueOrderByCreationDateDesc(Integer companyId);

    // MODIFICADO: Los métodos de validación de unicidad ahora NECESITAN el companyId.
    boolean existsByCompany_IdAndCustomerDui(Integer companyId, String dui);
    boolean existsByCompany_IdAndCustomerNit(Integer companyId, String nit);
    boolean existsByCompany_IdAndNcr(Integer companyId, String ncr);

    // MODIFICADO: Los métodos de validación para actualización también necesitan el companyId.
    boolean existsByCompany_IdAndCustomerDuiAndClientIdNot(Integer companyId, String dui, Integer clientId);
    boolean existsByCompany_IdAndCustomerNitAndClientIdNot(Integer companyId, String nit, Integer clientId);
    boolean existsByCompany_IdAndNcrAndClientIdNot(Integer companyId, String ncr, Integer clientId);

    // Para DUI
    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE customer_dui = :dui)", nativeQuery = true)
    boolean existsByDuiGlobal(@Param("dui") String dui);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE customer_dui = :dui AND client_id <> :clientId)", nativeQuery = true)
    boolean existsByDuiGlobalAndClientIdNot(@Param("dui") String dui, @Param("clientId") Integer clientId);

    // Para NIT
    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE customer_nit = :nit)", nativeQuery = true)
    boolean existsByNitGlobal(@Param("nit") String nit);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE customer_nit = :nit AND client_id <> :clientId)", nativeQuery = true)
    boolean existsByNitGlobalAndClientIdNot(@Param("nit") String nit, @Param("clientId") Integer clientId);

    // Para NCR
    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE ncr = :ncr)", nativeQuery = true)
    boolean existsByNcrGlobal(@Param("ncr") String ncr);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE ncr = :ncr AND client_id <> :clientId)", nativeQuery = true)
    boolean existsByNcrGlobalAndClientIdNot(@Param("ncr") String ncr, @Param("clientId") Integer clientId);

    // Para Email
    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE email = :email)", nativeQuery = true)
    boolean existsByEmailGlobal(@Param("email") String email);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE email = :email AND client_id <> :clientId)", nativeQuery = true)
    boolean existsByEmailGlobalAndClientIdNot(@Param("email") String email, @Param("clientId") Integer clientId);

    // Para Teléfono
    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE phone = :phone)", nativeQuery = true)
    boolean existsByPhoneGlobal(@Param("phone") String phone);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM customer WHERE phone = :phone AND client_id <> :clientId)", nativeQuery = true)
    boolean existsByPhoneGlobalAndClientIdNot(@Param("phone") String phone, @Param("clientId") Integer clientId);

}
