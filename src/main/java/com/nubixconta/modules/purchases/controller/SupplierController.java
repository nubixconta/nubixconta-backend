package com.nubixconta.modules.purchases.controller;

import com.nubixconta.modules.purchases.dto.supplier.SupplierCreateDTO;
import com.nubixconta.modules.purchases.dto.supplier.SupplierResponseDTO;
import com.nubixconta.modules.purchases.dto.supplier.SupplierUpdateDTO;
import com.nubixconta.modules.purchases.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    /**
     * Obtiene una lista de todos los proveedores activos.
     */
    @GetMapping
    public List<SupplierResponseDTO> getAllSuppliers() {
        return supplierService.findAll();
    }

    /**
     * Obtiene una lista de todos los proveedores inactivos.
     */
    @GetMapping("/inactive")
    public List<SupplierResponseDTO> getInactiveSuppliers() {
        return supplierService.findInactive();
    }

    /**
     * Busca proveedores activos por criterios de filtro opcionales.
     */
    @GetMapping("/search")
    public List<SupplierResponseDTO> searchActiveSuppliers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String dui,
            @RequestParam(required = false) String nit) {
        return supplierService.searchActive(name, lastName, dui, nit);
    }

    /**
     * Obtiene un proveedor específico por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> getSupplier(@PathVariable Integer id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    /**
     * Crea un nuevo proveedor.
     */
    @PostMapping
    public ResponseEntity<SupplierResponseDTO> createSupplier(@Valid @RequestBody SupplierCreateDTO dto) {
        SupplierResponseDTO created = supplierService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Actualiza parcialmente la información de un proveedor existente.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> updateSupplier(
            @PathVariable Integer id,
            @Valid @RequestBody SupplierUpdateDTO dto) {
        SupplierResponseDTO updated = supplierService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Desactiva un proveedor. Devuelve el estado actualizado del proveedor.
     */
    @PostMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public SupplierResponseDTO deactivateSupplier(@PathVariable Integer id) {
        supplierService.deactivate(id);
        // Retornamos la entidad actualizada para que el frontend pueda refrescar el estado
        return supplierService.findById(id);
    }

    /**
     * Reactiva un proveedor previamente desactivado. Devuelve el estado actualizado.
     */
    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.OK)
    public SupplierResponseDTO activateSupplier(@PathVariable Integer id) {
        supplierService.activate(id);
        // Retornamos la entidad actualizada
        return supplierService.findById(id);
    }
}