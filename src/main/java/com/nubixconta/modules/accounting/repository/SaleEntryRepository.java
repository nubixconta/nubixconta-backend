package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.SaleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleEntryRepository extends JpaRepository<SaleEntry, Integer> {
    // Elimina todos los SaleEntry que coincidan con el ID de la venta proporcionado.
    // JPA lo convierte en una operación de eliminación masiva, que es muy eficiente.
    void deleteBySale_SaleId(Integer saleId);
}