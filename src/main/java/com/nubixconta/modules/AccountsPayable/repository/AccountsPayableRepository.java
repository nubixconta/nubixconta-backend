package com.nubixconta.modules.AccountsPayable.repository;

import com.nubixconta.modules.AccountsPayable.entity.AccountsPayable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountsPayableRepository extends JpaRepository<AccountsPayable, Integer>, JpaSpecificationExecutor<AccountsPayable> {

    // Creamos un nuevo método para encontrar todas las cuentas por cobrar de una empresa
    List<AccountsPayable> findByCompanyId(Integer companyId);
}
