package com.nubixconta.modules.sales.controller;

import com.nubixconta.modules.sales.dto.customer.*;
import com.nubixconta.modules.sales.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    /**
     * MODIFICADO: Se elimina el parámetro HttpServletRequest.
     * El servicio ahora obtiene toda la información de contexto necesaria (companyId)
     * de forma segura desde el TenantContext, haciendo el controlador más limpio.
     */
    @PostMapping
    public ResponseEntity<CustomerResponseDTO> createCustomer(@Valid @RequestBody CustomerCreateDTO dto) {
        CustomerResponseDTO created = customerService.save(dto);
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

    // --- NUEVO ENDPOINT (Consistente con SaleController): Desactivar un cliente ---
    @PostMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public CustomerResponseDTO deactivateCustomer(@PathVariable Integer id) {
        customerService.deactivate(id);
        // Devolvemos el cliente actualizado para que el frontend pueda refrescar el estado
        return customerService.findById(id);
    }

    // --- NUEVO ENDPOINT (Consistente con SaleController): Reactivar un cliente ---
    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.OK)
    public CustomerResponseDTO activateCustomer(@PathVariable Integer id) {
        customerService.activate(id);
        // Devolvemos el cliente actualizado
        return customerService.findById(id);
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