package com.nubixconta.modules.inventory.repository;

import com.nubixconta.modules.inventory.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Integer> {
    List<InventoryMovement> findByProduct_IdProduct(Integer idProduct);
}