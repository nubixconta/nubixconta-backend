package com.nubixconta.modules.sales.repository;
import com.nubixconta.modules.sales.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
public interface SaleRepository extends JpaRepository<Sale, Integer>{
    // TODO: Agregar métodos personalizados si se necesitan
    //  ya que JpaRepository trae ya metodos para manipular la bd
    // Buscar ventas por rango de fechas de emisión
    @Query("SELECT s FROM Sale s WHERE s.issueDate >= :start AND s.issueDate <= :end")
    List<Sale> findByIssueDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    @Query("SELECT s FROM Sale s WHERE s.customer.clientId IN :customerIds")
    List<Sale> findByCustomerIds(@Param("customerIds") List<Integer> customerIds);
}
