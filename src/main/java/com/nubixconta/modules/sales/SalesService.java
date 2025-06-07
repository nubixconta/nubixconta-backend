package com.nubixconta.modules.sales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SalesService {
    @Autowired
    private SalesRepository salesRepository;

    // TODO: Reemplazar con lógica real cuando se definan los requerimientos

    /*public Sales save(Sales sale) {
        // Lógica para guardar una venta
        return salesRepository.save(sale);
    }

    public List<Sales> findAll() {
        return salesRepository.findAll();
    }*/
}
