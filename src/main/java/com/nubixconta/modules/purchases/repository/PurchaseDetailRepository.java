package com.nubixconta.modules.purchases.repository;

import com.nubixconta.modules.purchases.entity.PurchaseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseDetailRepository extends JpaRepository<PurchaseDetail, Integer> {

    // Método útil para obtener todos los detalles de una compra específica
    List<PurchaseDetail> findByPurchase_IdPurchase(Integer purchaseId);
}