package com.nubixconta.modules.inventory.service;

import com.nubixconta.modules.inventory.entity.InventoryMovement;
import com.nubixconta.modules.inventory.repository.InventoryMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryMovementService {
    private final InventoryMovementRepository inventoryMovementRepository;

    public List<InventoryMovement> findAll() {
        return inventoryMovementRepository.findAll();
    }

    public Optional<InventoryMovement> findById(Integer id) {
        return inventoryMovementRepository.findById(id);
    }

    public List<InventoryMovement> findByProductId(Integer productId) {
        return inventoryMovementRepository.findByProduct_IdProduct(productId);
    }

    public InventoryMovement save(InventoryMovement movement) {
        return inventoryMovementRepository.save(movement);
    }

    public void delete(Integer id) {
        inventoryMovementRepository.deleteById(id);
    }
}