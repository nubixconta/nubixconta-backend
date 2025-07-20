package com.nubixconta.modules.accountsreceivable.repository;

import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountsReceivableRepository extends JpaRepository<AccountsReceivable, Integer>, JpaSpecificationExecutor<AccountsReceivable>{
 //Selecciona un rango de fecha
   @Query("SELECT a FROM AccountsReceivable a WHERE a.receivableAccountDate BETWEEN :start AND :end")
    List<AccountsReceivable> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
