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



    // --- INICIO: NUEVOS MÉTODOS OPTIMIZADOS CON JOIN FETCH ---

    /**
     * Busca movimientos por rango de fechas, cargando eficientemente todas las relaciones necesarias
     * para el DTO en una sola consulta.
     */
    @Query("SELECT m FROM InventoryMovement m " +
            "LEFT JOIN FETCH m.product " +
            "LEFT JOIN FETCH m.sale s LEFT JOIN FETCH s.customer " +
            "LEFT JOIN FETCH m.creditNote cn LEFT JOIN FETCH cn.sale cns LEFT JOIN FETCH cns.customer " +
            "WHERE m.company.id = :companyId AND m.date BETWEEN :startDate AND :endDate " +
            "ORDER BY m.date DESC")
    List<InventoryMovement> findByCompanyIdAndDateBetweenWithDetails(
            @Param("companyId") Integer companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Busca todos los movimientos ordenados por estado y fecha, cargando eficientemente
     * todas las relaciones necesarias para el DTO en una sola consulta.
     */
    @Query("SELECT m FROM InventoryMovement m " +
            "LEFT JOIN FETCH m.product " +
            "LEFT JOIN FETCH m.sale s LEFT JOIN FETCH s.customer " +
            "LEFT JOIN FETCH m.creditNote cn LEFT JOIN FETCH cn.sale cns LEFT JOIN FETCH cns.customer " +
            "WHERE m.company.id = :companyId ORDER BY " +
            "CASE m.status WHEN 'PENDIENTE' THEN 1 WHEN 'APLICADA' THEN 2 WHEN 'ANULADA' THEN 3 ELSE 4 END, " +
            "m.date DESC")
    List<InventoryMovement> findAllByCompanyIdOrderByStatusAndDateWithDetails(@Param("companyId") Integer companyId);

    /**
     * Busca todos los movimientos ordenados por fecha, cargando eficientemente
     * todas las relaciones necesarias para el DTO en una sola consulta.
     */
    @Query("SELECT m FROM InventoryMovement m " +
            "LEFT JOIN FETCH m.product " +
            "LEFT JOIN FETCH m.sale s LEFT JOIN FETCH s.customer " +
            "LEFT JOIN FETCH m.creditNote cn LEFT JOIN FETCH cn.sale cns LEFT JOIN FETCH cns.customer " +
            "WHERE m.company.id = :companyId ORDER BY m.date DESC")
    List<InventoryMovement> findByCompanyIdOrderByDateDescWithDetails(@Param("companyId") Integer companyId);

    // --- FIN: NUEVOS MÉTODOS OPTIMIZADOS ---
}