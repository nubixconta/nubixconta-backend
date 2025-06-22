package com.nubixconta.modules.sales.repository;

import com.nubixconta.modules.sales.entity.SaleDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleDetailRepository extends JpaRepository<SaleDetail, Integer> {
    // Ejemplo: obtener detalles por venta
    List<SaleDetail> findBySale_SaleId(Integer saleId);
}