package com.nubixconta.modules.AccountsPayable.repository;

import com.nubixconta.modules.AccountsPayable.entity.AccountsPayable;
import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.modules.sales.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountsPayableRepository extends JpaRepository<AccountsPayable, Integer>, JpaSpecificationExecutor<AccountsPayable> {

    // Creamos un nuevo método para encontrar todas las cuentas por cobrar de una empresa
    List<AccountsPayable> findByCompanyId(Integer companyId);

    // Modificamos el método para incluir el filtro de empresa
    @Query("SELECT ar FROM AccountsPayable ar WHERE ar.company.id = :companyId AND ar.purchaseId = :purchaseId")
    Optional<AccountsPayable> findByPurchaseIdAndCompanyId(@Param("purchaseId") Integer purchaseId, @Param("companyId") Integer companyId);

    Optional<AccountsPayable> findByPurchase(Purchase purchse);
    @Query("SELECT ap FROM AccountsPayable ap JOIN FETCH ap.paymentDetails WHERE ap.company.id = :companyId")
    List<AccountsPayable> findByCompanyIdWithDetails(@Param("companyId") Integer companyId);

  /*  @Query("SELECT ar FROM AccountsPayable ar JOIN FETCH ar.paymentDetails cd WHERE ar.purcharse.idPurchase = :idPurchase AND ar.company.id = :companyId")
    Optional<AccountsPayable> findByPurchase_idPurchaseAndCompany_IdWithPayableDetails(@Param("idPurchase") Integer idPurchase, @Param("companyId") Integer companyId);*/
}







