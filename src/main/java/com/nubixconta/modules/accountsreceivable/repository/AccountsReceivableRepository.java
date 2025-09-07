package com.nubixconta.modules.accountsreceivable.repository;

import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.sales.entity.Sale;
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

    Optional<AccountsReceivable> findBySale(Sale sale);
   
    @Query("SELECT ar FROM AccountsReceivable ar JOIN FETCH ar.collectionDetails cd WHERE ar.sale.saleId = :saleId AND ar.company.id = :companyId")
    Optional<AccountsReceivable> findBySale_SaleIdAndCompany_IdWithCollectionDetails(@Param("saleId") Integer saleId, @Param("companyId") Integer companyId);
}
