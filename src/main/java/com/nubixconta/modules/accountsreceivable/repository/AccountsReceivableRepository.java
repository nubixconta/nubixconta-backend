package com.nubixconta.modules.accountsreceivable.repository;

import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountsReceivableRepository extends JpaRepository<AccountsReceivable, Integer>, JpaSpecificationExecutor<AccountsReceivable>{
    // Modificamos el método para incluir el filtro de empresa
    @Query("SELECT ar FROM AccountsReceivable ar WHERE ar.company.id = :companyId AND ar.saleId = :saleId")
    Optional<AccountsReceivable> findBySaleIdAndCompanyId(@Param("saleId") Integer saleId, @Param("companyId") Integer companyId);

    // Creamos un nuevo método para encontrar todas las cuentas por cobrar de una empresa
    List<AccountsReceivable> findByCompanyId(Integer companyId);

    // Modificamos el método findById para que también filtre por empresa
    Optional<AccountsReceivable> findByIdAndCompanyId(Integer id, Integer companyId);
}
