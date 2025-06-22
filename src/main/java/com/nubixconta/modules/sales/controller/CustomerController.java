package com.nubixconta.modules.sales.controller;

import com.nubixconta.modules.sales.entity.Customer;
import com.nubixconta.modules.sales.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable Integer id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        // Si hay errores de validación, el GlobalExceptionHandler responderá automáticamente
        customer.setClientId(null); // Asegura que no venga el id en creación
        return ResponseEntity.ok(customerService.save(customer));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> updates) {

        Optional<Customer> optionalCustomer = customerService.findById(id);
        if (optionalCustomer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Customer customer = optionalCustomer.get();

        updates.forEach((key, value) -> {
            // No permitir modificar el id
            if (key.equalsIgnoreCase("clientId")) {
                return;
            }
            Field field = ReflectionUtils.findField(Customer.class, key);
            if (field != null) {
                field.setAccessible(true);
                ReflectionUtils.setField(field, customer, value);
            }
        });


        return ResponseEntity.ok(customerService.save(customer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        try {
            customerService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // 1. Búsqueda flexible SOLO activos
    @GetMapping("/search")
    public List<Customer> searchActiveCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String dui,
            @RequestParam(required = false) String nit
    ) {
        return customerService.searchActive(name, lastName, dui, nit);
    }

    // 2. Listar clientes desactivados
    @GetMapping("/inactive")
    public List<Customer> getInactiveCustomers() {
        return customerService.findInactive();
    }

}