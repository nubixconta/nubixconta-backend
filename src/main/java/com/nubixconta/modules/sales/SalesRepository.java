package com.nubixconta.modules.sales;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesRepository extends JpaRepository<Sales, Long>{
    // TODO: Agregar métodos personalizados si se necesitan
    //  ya que JpaRepositor trae ya metodos para manipular la bd

}
