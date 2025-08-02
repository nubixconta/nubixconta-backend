package com.nubixconta.modules.inventory.repository;

import com.nubixconta.modules.inventory.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Integer> {
    // La búsqueda por rango de fechas ahora debe estar acotada a una empresa.
    List<InventoryMovement> findByCompany_IdAndDateBetween(Integer companyId, LocalDateTime startDate, LocalDateTime endDate);

}