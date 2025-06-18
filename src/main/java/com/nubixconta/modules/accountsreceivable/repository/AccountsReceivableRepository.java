package com.nubixconta.modules.accountsreceivable.repository;
import com.nubixconta.modules.accountsreceivable.AccountsReceivable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountsReceivableRepository extends JpaRepository<AccountsReceivable, Long>{
    // TODO: Agregar métodos personalizados si se necesitan
    //  ya que JpaRepositor trae ya metodos para manipular la bd
}
