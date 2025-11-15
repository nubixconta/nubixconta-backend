package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.PaymentEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentEntryRepository extends JpaRepository<PaymentEntry, Integer> {
    void deleteByPaymentDetailsId(Integer paymentDetailId);
    //  método para encontrar todas las entradas por el ID del detalle
    List<PaymentEntry> findByPaymentDetails_Id(Integer paymentDetailId);
    List<PaymentEntry> findAll();

    @Query("SELECT pe FROM PaymentEntry pe JOIN FETCH pe.catalog c JOIN FETCH c.account a")
    List<PaymentEntry> findAllWithCatalogAndAccount();
}
