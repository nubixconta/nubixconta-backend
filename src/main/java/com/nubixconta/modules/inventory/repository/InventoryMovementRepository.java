package com.nubixconta.modules.inventory.repository;

import com.nubixconta.modules.inventory.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Integer> {
    // La búsqueda por rango de fechas ahora debe estar acotada a una empresa.
    List<InventoryMovement> findByCompany_IdAndDateBetween(Integer companyId, LocalDateTime startDate, LocalDateTime endDate);
    // NUEVO MÉTODO: Basado en tu estrategia del módulo de Ventas.
    // Ordena primero por el estado del movimiento y luego por fecha descendente.
    @Query("SELECT m FROM InventoryMovement m WHERE m.company.id = :companyId ORDER BY " +
            "CASE m.status " +
            "  WHEN 'PENDIENTE' THEN 1 " +
            "  WHEN 'APLICADA'  THEN 2 " +
            "  WHEN 'ANULADA'   THEN 3 " +
            "  ELSE 4 " +
            "END, " +
            "m.date DESC")
    List<InventoryMovement> findAllByCompanyIdOrderByStatusAndDate(@Param("companyId") Integer companyId);

    // NUEVO MÉTODO (Opcional pero recomendado): Un ordenamiento simple por fecha.
    List<InventoryMovement> findByCompany_IdOrderByDateDesc(Integer companyId);
}