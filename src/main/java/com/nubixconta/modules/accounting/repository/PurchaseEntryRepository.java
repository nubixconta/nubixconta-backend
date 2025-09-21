package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.PurchaseEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query; // <-- Asegúrate de tener este import
import java.util.List; // <-- Y este también

@Repository
public interface PurchaseEntryRepository extends JpaRepository<PurchaseEntry, Integer> {

    /**
     * Elimina todas las líneas del asiento contable asociadas a un ID de compra específico.
     * Esencial para el método de anulación de compras.
     *
     * @param purchaseId El ID de la compra cuyas entradas de asiento se eliminarán.
     */
    void deleteByPurchase_IdPurchase(Integer purchaseId);

    /**
     * Busca todas las líneas del asiento para una compra específica, cargando
     * eficientemente las entidades anidadas Catalog y Account para evitar N+1.
     * @param purchaseId El ID de la compra.
     * @return Una lista de las entidades PurchaseEntry con sus datos relacionados precargados.
     */
    @Query("SELECT pe FROM PurchaseEntry pe " +
            "JOIN FETCH pe.catalog c " +
            "JOIN FETCH c.account " +
            "WHERE pe.purchase.idPurchase = :purchaseId")
    List<PurchaseEntry> findByPurchaseIdWithDetails(Integer purchaseId);
}