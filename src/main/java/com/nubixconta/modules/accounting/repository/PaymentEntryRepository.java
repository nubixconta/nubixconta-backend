package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.PaymentEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentEntryRepository extends JpaRepository<PaymentEntry, Integer> {
    void deleteByPaymentDetailsId(Integer paymentDetailId);
    //  método para encontrar todas las entradas por el ID del detalle
    List<PaymentEntry> findByPaymentDetails_Id(Integer paymentDetailId);
}
