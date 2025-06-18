package com.nubixconta.modules.sales.repository;
import com.nubixconta.modules.sales.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Integer>{
    // TODO: Agregar métodos personalizados si se necesitan
    //  ya que JpaRepositor trae ya metodos para manipular la bd

}
