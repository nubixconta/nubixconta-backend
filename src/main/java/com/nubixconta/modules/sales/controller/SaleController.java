package com.nubixconta.modules.sales.controller;

import com.nubixconta.common.exception.BadRequestException;
import com.nubixconta.modules.sales.dto.customer.SaleForAccountsReceivableDTO;
import com.nubixconta.modules.sales.dto.sales.SaleCreateDTO;
import com.nubixconta.modules.sales.dto.sales.SaleResponseDTO;
import com.nubixconta.modules.sales.dto.sales.SaleUpdateDTO;
import com.nubixconta.modules.sales.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    /**
     * Obtener todas las ventas registradas.
     * Devuelve una lista de SaleResponseDTO.
     */
    @GetMapping
    public ResponseEntity<List<SaleResponseDTO>> getAllSales() {
        List<SaleResponseDTO> sales = saleService.findAll();
        return ResponseEntity.ok(sales);
    }

    /**
     * Obtener una venta por ID, incluyendo sus detalles.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SaleResponseDTO> getSaleById(@PathVariable Integer id) {
        SaleResponseDTO sale = saleService.findById(id);
        return ResponseEntity.ok(sale);
    }

    /**
     * Crear una nueva venta junto con sus detalles.
     * Se valida:
     *  - Que el cliente exista
     *  - Que los productos existan
     *  - Que cada detalle tenga solo producto o servicio, no ambos
     */
    @PostMapping
    public ResponseEntity<SaleResponseDTO> createSale(@Valid @RequestBody SaleCreateDTO saleCreateDTO) {
        SaleResponseDTO createdSale = saleService.createSale(saleCreateDTO);
        return ResponseEntity.ok(createdSale);
    }

    /**
     * Eliminar una venta por ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Integer id) {
        saleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Buscar ventas por rango de fechas.
     * Las fechas deben ser enviadas en formato ISO (yyyy-MM-dd).
     */
    @GetMapping("/search")
    public ResponseEntity<List<SaleResponseDTO>> searchSalesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<SaleResponseDTO> sales = saleService.findByIssueDateBetween(start, end);
        return ResponseEntity.ok(sales);
    }

    /**
     * Obtener los detalles de una venta específica.
     * Este endpoint es opcional, ya que normalmente los detalles vienen embebidos en SaleResponseDTO.
     */
    @GetMapping("/{saleId}/details")
    public ResponseEntity<List<?>> getSaleDetailsBySale(@PathVariable Integer saleId) {
        SaleResponseDTO sale = saleService.findById(saleId);
        return ResponseEntity.ok(sale.getSaleDetails());
    }

    //endpoint para actualizar una venta
    @PatchMapping("/{id}")
    public ResponseEntity<SaleResponseDTO> updateSale(@PathVariable Integer id,@RequestBody SaleUpdateDTO updateDTO) {
        SaleResponseDTO updated = saleService.updateSalePartial(id, updateDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/client-search")
    public ResponseEntity<List<SaleResponseDTO>> searchSalesByCustomer(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String dui,
            @RequestParam(required = false) String nit
    ) {
        if ((name == null || name.isBlank()) &&
                (lastName == null || lastName.isBlank()) &&
                (dui == null || dui.isBlank()) &&
                (nit == null || nit.isBlank())
        ) {
            throw new BadRequestException("Debe enviar al menos un criterio de búsqueda.");
        }
        List<SaleResponseDTO> results = saleService.findByCustomerSearch(name, lastName, dui, nit);
        return ResponseEntity.ok(results);
    }
    @GetMapping("/receivables")
    public ResponseEntity<List<SaleForAccountsReceivableDTO>> getSalesForAccountsReceivable() {
        List<SaleForAccountsReceivableDTO> sales = saleService.findSalesForAccountsReceivable();
        return ResponseEntity.ok(sales);
    }

}