package com.nubixconta.modules.accountsreceivable.repository;
import com.nubixconta.modules.accountsreceivable.AccountsReceivableService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountsReceivableRepository extends JpaRepository<AccountsReceivableService, Long>{
    // TODO: Agregar métodos personalizados si se necesitan
    //  ya que JpaRepositor trae ya metodos para manipular la bd
}
