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

    boolean existsByDocumentNumber(String documentNumber);

    List<Sale> findBySaleStatus(String saleStatus);

    List<Sale> findAllByOrderByIssueDateDesc();

    @Query("SELECT s FROM Sale s ORDER BY " +
            "CASE s.saleStatus " +
            "  WHEN 'PENDIENTE' THEN 1 " +
            "  WHEN 'APLICADA'  THEN 2 " +
            "  WHEN 'ANULADA'   THEN 3 " +
            "  ELSE 4 " +
            "END, " +
            "s.issueDate DESC")
    List<Sale> findAllOrderByStatusAndIssueDate();

    /**
     * Busca todas las ventas de un cliente específico que coincidan con un estado determinado.
     * Spring Data JPA genera la consulta:
     * "SELECT s FROM Sale s WHERE s.customer.clientId = ?1 AND s.saleStatus = ?2"
     *
     * @param clientId El ID del cliente a buscar.
     * @param saleStatus El estado de la venta (ej. "APLICADA").
     * @return Una lista de ventas que cumplen ambos criterios.
     */
    List<Sale> findByCustomer_ClientIdAndSaleStatus(Integer clientId, String saleStatus);

    /**
     * Busca ventas aplicadas de un cliente que no tienen una nota de crédito activa (PENDIENTE o APLICADA).
     * Esta es la consulta definitiva para encontrar ventas a las que se les puede crear una nota de crédito.
     * @param clientId El ID del cliente.
     * @return Una lista de ventas válidas para una nota de crédito.
     */
    @Query("SELECT s FROM Sale s WHERE s.customer.clientId = :clientId AND s.saleStatus = 'APLICADA' " +
            "AND NOT EXISTS (SELECT 1 FROM CreditNote cn WHERE cn.sale = s AND cn.creditNoteStatus IN ('PENDIENTE', 'APLICADA'))")
    List<Sale> findSalesAvailableForCreditNote(@Param("clientId") Integer clientId);

}
