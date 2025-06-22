package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.sales.entity.Customer;
import com.nubixconta.modules.sales.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public List<Customer> findAll() {
        return customerRepository.findByStatusTrue();
    }

    public Optional<Customer> findById(Integer id) {
        return customerRepository.findById(id);
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    public void delete(Integer id) {
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        customerRepository.deleteById(id);
    }
    // Nuevo: Búsqueda flexible SOLO clientes activos
    public List<Customer> searchActive(String name, String lastName, String dui, String nit) {
        // Si todos los parámetros son null, se devuelven todos los activos.
        return customerRepository.searchActive(
                emptyToNull(name), emptyToNull(lastName), emptyToNull(dui), emptyToNull(nit)
        );
    }

    // Nuevo: Obtener clientes desactivados
    public List<Customer> findInactive() {
        return customerRepository.findByStatusFalse();
    }

    // Ayuda para no filtrar por cadenas vacías
    private String emptyToNull(String value) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }
}