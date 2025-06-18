package com.nubixconta.modules.sales.service;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepository;

    public List<Sale> findAll() {
        return saleRepository.findAll();
    }

    public Optional<Sale> findById(Integer id) {
        return saleRepository.findById(id);
    }

    public Sale save(Sale sale) {
        return saleRepository.save(sale);
    }

    public void delete(Integer  id) {
        if (!saleRepository.existsById(id)) {
            throw new IllegalArgumentException("Venta no encontrada");
        }
        saleRepository.deleteById(id);
    }
}
