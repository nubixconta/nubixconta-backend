package com.nubixconta.modules.AccountsPayable.repository;

import com.nubixconta.modules.AccountsPayable.entity.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Integer> {
    // Nuevo método para encontrar todos por empresa
    List<PaymentDetails> findByCompanyId(Integer companyId);
}
