package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.IncomeTaxEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeTaxEntryRepository extends JpaRepository<IncomeTaxEntry, Integer> {

    /**
     * Elimina todas las líneas de asiento asociadas a un ID de retención de ISR específico.
     * Esencial para la operación de anulación.
     */
    void deleteByIncomeTax_IdIncomeTax(Integer incomeTaxId);

    /**
     * Busca todas las líneas del asiento para una retención de ISR específica, cargando
     * eficientemente las entidades anidadas Catalog y Account para evitar N+1.
     * @param incomeTaxId El ID de la retención de ISR.
     * @return Una lista de las entidades IncomeTaxEntry con sus datos relacionados precargados.
     */
    @Query("SELECT ite FROM IncomeTaxEntry ite " +
            "JOIN FETCH ite.catalog c " +
            "JOIN FETCH c.account " +
            "WHERE ite.incomeTax.idIncomeTax = :incomeTaxId")
    List<IncomeTaxEntry> findByIncomeTaxIdWithDetails(@Param("incomeTaxId") Integer incomeTaxId);
}