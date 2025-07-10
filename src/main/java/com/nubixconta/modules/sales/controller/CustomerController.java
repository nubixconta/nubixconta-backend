package com.nubixconta.modules.sales.controller;

import com.nubixconta.modules.sales.dto.customer.*;
import com.nubixconta.modules.sales.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;



@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // Obtener todos los clientes activos
    @GetMapping
    public List<CustomerResponseDTO> getAllCustomers() {
        return customerService.findAll();
    }

    // Obtener cliente por ID (si no existe, lanza NotFoundException)
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> getCustomer(@PathVariable Integer id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    // Crear nuevo cliente (validado y asociado a usuario autenticado)
    @PostMapping
    public ResponseEntity<CustomerResponseDTO> createCustomer(
            @Valid @RequestBody CustomerCreateDTO dto,
            HttpServletRequest request) {
        CustomerResponseDTO created = customerService.save(dto, request);
        return ResponseEntity.status(201).body(created);
    }

    // Actualizar cliente (parcial)
    @PatchMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> updateCustomer(
            @PathVariable Integer id,
            @RequestBody CustomerUpdateDTO dto) {
        CustomerResponseDTO updated = customerService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    // Eliminar cliente
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Buscar clientes activos con filtros
    @GetMapping("/search")
    public List<CustomerResponseDTO> searchActiveCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String dui,
            @RequestParam(required = false) String nit) {
        return customerService.searchActive(name, lastName, dui, nit);
    }

    // Obtener clientes inactivos
    @GetMapping("/inactive")
    public List<CustomerResponseDTO> getInactiveCustomers() {
        return customerService.findInactive();
    }
}