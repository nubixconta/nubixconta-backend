package com.nubixconta.modules.AccountsPayable.repository;

import com.nubixconta.modules.AccountsPayable.entity.PaymentDetails;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Integer> {
    // Nuevo método para encontrar todos por empresa
    List<PaymentDetails> findByCompany_Id(Integer companyId);

    @Query("SELECT a FROM PaymentDetails a WHERE a.accountsPayable.id = :payableId AND a.company.id = :companyId")
    List<PaymentDetails> findByAccountsPayableAndCompanyId(@Param("payableId") Integer payableId, @Param("companyId") Integer companyId);
}
