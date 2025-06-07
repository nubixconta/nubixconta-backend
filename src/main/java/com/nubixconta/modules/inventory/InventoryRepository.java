package com.nubixconta.modules.inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long>{
    // TODO: Agregar métodos personalizados si se necesitan
    //  ya que JpaRepository trae ya metodos para manipular la bd
}
