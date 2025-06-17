package com.nubixconta.modules.sales.service;
import com.nubixconta.modules.sales.entity.SaleDetail;
import com.nubixconta.modules.sales.repository.SaleDetailRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleDetailService {
    private final SaleDetailRepository saleDetailRepository;

    public List<SaleDetail> findAll() {
        return saleDetailRepository.findAll();
    }
    public SaleDetail findById(Integer id) {
        return saleDetailRepository.findById(id).orElse(null);
    }
    public SaleDetail save(SaleDetail saleDetail) {
        return saleDetailRepository.save(saleDetail);
    }
    public void delete(Integer id) {
        saleDetailRepository.deleteById(id);
    }
}

