package com.nubixconta.modules.administration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministrationRepository extends JpaRepository<Administration, Long> {
    // TODO: Agregar métodos personalizados si se necesitan
    //  ya que JpaRepository trae ya metodos para manipular la bd
}
