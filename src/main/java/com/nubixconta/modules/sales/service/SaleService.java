package com.nubixconta.modules.sales.service;
import com.nubixconta.modules.sales.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SaleService {
    @Autowired
    private SaleRepository salesRepository;

    // TODO: Reemplazar con lógica real cuando se definan los requerimientos

    /*public Sales save(Sales sale) {
        // Lógica para guardar una venta
        return salesRepository.save(sale);
    }

    public List<Sales> findAll() {
        return salesRepository.findAll();
    }*/
}
