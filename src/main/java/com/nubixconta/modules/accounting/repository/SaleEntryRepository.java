package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.SaleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SaleEntryRepository extends JpaRepository<SaleEntry, Integer> {
    // Elimina todos los SaleEntry que coincidan con el ID de la venta proporcionado.
    // JPA lo convierte en una operación de eliminación masiva, que es muy eficiente.
    void deleteBySale_SaleId(Integer saleId);

    /**
     * Busca todas las líneas del asiento para una venta específica, cargando
     * eficientemente las entidades anidadas Catalog y Account para evitar N+1.
     * El uso de JOIN FETCH es una optimización crítica para el rendimiento.
     * @param saleId El ID de la venta.
     * @return Una lista de las entidades SaleEntry con sus datos relacionados precargados.
     */
    @Query("SELECT se FROM SaleEntry se " +
            "JOIN FETCH se.catalog c " +
            "JOIN FETCH c.account " +
            "WHERE se.sale.saleId = :saleId")
    List<SaleEntry> findBySaleIdWithDetails(Integer saleId);
}