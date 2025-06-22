package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.sales.entity.Customer;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepository;
    private final CustomerService customerService;

    public List<Sale> findAll() {
        return saleRepository.findAll();
    }

    public Optional<Sale> findById(Integer id) {
        return saleRepository.findById(id);
    }

    public Sale save(Sale sale) {
        return saleRepository.save(sale);
    }

    public void delete(Integer id) {
        if (!saleRepository.existsById(id)) {
            throw new IllegalArgumentException("Venta no encontrada");
        }
        saleRepository.deleteById(id);
    }
    // Ventas por rango de fechas
    public List<Sale> findByIssueDateBetween(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        return saleRepository.findByIssueDateBetween(startDateTime, endDateTime);
    }

    // Buscar ventas por criterios de cliente (nombre, apellido, dui, nit)
    public List<Sale> findByCustomerSearch(
            String name, String lastName, String dui, String nit
    ) {
        List<Customer> customers = customerService.searchActive(name, lastName, dui, nit);
        if (customers.isEmpty()) {
            return List.of();
        }
        List<Integer> customerIds = customers.stream()
                .map(Customer::getClientId)
                .toList();
        return saleRepository.findByCustomerIds(customerIds);
    }
    private String emptyToNull(String value) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }
}
