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
    /**
     * Busca ventas por rango de fechas y las ordena por fecha de emisión descendente (la más nueva primero).
     * Spring Data JPA genera la consulta automáticamente gracias al nombre del método.
     * Consulta generada: "SELECT s FROM Sale s WHERE s.issueDate BETWEEN :start AND :end ORDER BY s.issueDate DESC"
     */
    List<Sale> findByIssueDateBetweenOrderByIssueDateDesc(LocalDateTime start, LocalDateTime end);


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

    /**
     * Busca ventas APLICADAS combinando múltiples criterios opcionales y las ordena por fecha de emisión.
     * Este es el método definitivo para el reporte de ventas.
     * @param startDate Fecha de inicio del rango (puede ser null).
     * @param endDate Fecha de fin del rango (puede ser null).
     * @param customerName Nombre del cliente (puede ser null, busca coincidencias parciales).
     * @param customerLastName Apellido del cliente (puede ser null, busca coincidencias parciales).
     * @return Una lista de ventas APLICADAS que cumplen con todos los criterios y están ordenadas por fecha descendente.
     */
    @Query(value = "SELECT s.* FROM sale s JOIN customer c ON s.client_id = c.client_id WHERE " +
            "s.sale_status = 'APLICADA' AND " +
            "s.issue_date >= COALESCE(:startDate, s.issue_date) AND " +
            "s.issue_date <= COALESCE(:endDate, s.issue_date) AND " +
            "COALESCE(LOWER(CAST(c.customer_name AS VARCHAR)), '') LIKE LOWER(CONCAT('%', COALESCE(:customerName, ''), '%')) AND " +
            "COALESCE(LOWER(CAST(c.customer_last_name AS VARCHAR)), '') LIKE LOWER(CONCAT('%', COALESCE(:customerLastName, ''), '%')) " +
            "ORDER BY s.issue_date DESC",
            nativeQuery = true)
    List<Sale> findByCombinedCriteria(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("customerName") String customerName,
            @Param("customerLastName") String customerLastName
    );

}
