package com.nubixconta.modules.inventory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    @Autowired
    private InventoryRepository inventoryRepository;

    // TODO: Agregar lógica de negocio para inventario
}
