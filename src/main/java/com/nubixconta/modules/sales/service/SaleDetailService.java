package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.sales.entity.SaleDetail;
import com.nubixconta.modules.sales.repository.SaleDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaleDetailService {
    private final SaleDetailRepository saleDetailRepository;

    public List<SaleDetail> findAll() {
        return saleDetailRepository.findAll();
    }

    public Optional<SaleDetail> findById(Integer id) {
        return saleDetailRepository.findById(id);
    }

    public List<SaleDetail> findBySaleId(Integer saleId) {
        return saleDetailRepository.findBySale_SaleId(saleId);
    }

    public SaleDetail save(SaleDetail saleDetail) {
        return saleDetailRepository.save(saleDetail);
    }


    public void delete(Integer id) {
        if (!saleDetailRepository.existsById(id)) {
            throw new IllegalArgumentException("Detalle de venta no encontrado");
        }
        saleDetailRepository.deleteById(id);
    }
}


