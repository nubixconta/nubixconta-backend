package com.nubixconta.modules.sales.controller;

import com.nubixconta.common.exception.BadRequestException;
import com.nubixconta.modules.sales.dto.sales.SaleForAccountsReceivableDTO;
import com.nubixconta.modules.sales.dto.sales.SaleCreateDTO;
import com.nubixconta.modules.sales.dto.sales.SaleResponseDTO;
import com.nubixconta.modules.sales.dto.sales.SaleUpdateDTO;
import com.nubixconta.modules.sales.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
     * Obtener todas las ventas registradas con un ordenamiento específico.
     * @param sortBy (opcional) Criterio de ordenamiento.
     *               - 'status' (default): Agrupa por PENDIENTE, APLICADA, ANULADA y luego ordena por fecha.
     *               - 'date': Ordena estrictamente por fecha de emisión descendente.
     * @return Una lista de SaleResponseDTO ordenadas según el criterio.
     */
    @GetMapping
    public ResponseEntity<List<SaleResponseDTO>> getAllSales(
            @RequestParam(name = "sortBy", defaultValue = "status") String sortBy
    ) {
        // Valida que el valor de sortBy sea uno de los permitidos para evitar comportamientos inesperados.
        if (!"status".equalsIgnoreCase(sortBy) && !"date".equalsIgnoreCase(sortBy)) {
            throw new BadRequestException("Valor de 'sortBy' no válido. Use 'status' o 'date'.");
        }

        List<SaleResponseDTO> sales = saleService.findAll(sortBy);
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

    /**Busca ventas por su estado= PENDIENTE,ANULADA,APLICADA*/
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SaleResponseDTO>> getSalesByStatus(@PathVariable String status) {
        List<SaleResponseDTO> sales = saleService.findByStatus(status);
        return ResponseEntity.ok(sales);
    }

    /**
     * Busca y devuelve las ventas de un cliente que están disponibles para crear una nota de crédito.
     * Filtra las ventas que ya tienen una nota de crédito activa (PENDIENTE o APLICADA).
     *
     * @param clientId El ID del cliente.
     * @return Una lista de SaleResponseDTO con las ventas elegibles.
     */
    // La ruta ahora es más clara sobre lo que devuelve.
    @GetMapping("/customer/{clientId}/available-for-credit-note")
    public ResponseEntity<List<SaleResponseDTO>> getSalesAvailableForCreditNote(@PathVariable Integer clientId) {
        // Llama al nuevo método del servicio.
        List<SaleResponseDTO> sales = saleService.findSalesAvailableForCreditNote(clientId);
        return ResponseEntity.ok(sales);
    }
    /**
     * Realiza una búsqueda combinada de ventas por rango de fechas y/o criterios de cliente.
     * Todos los parámetros son opcionales.
     */
    @GetMapping("/report")
    public ResponseEntity<List<SaleResponseDTO>> searchSalesCombined(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerLastName
    ) {
        List<SaleResponseDTO> sales = saleService.findByCombinedCriteria(
                startDate, endDate, customerName, customerLastName
        );
        return ResponseEntity.ok(sales);
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

    @PostMapping("/{id}/apply")
    @ResponseStatus(HttpStatus.OK)
    public SaleResponseDTO applySale(@PathVariable Integer id) {
        return saleService.applySale(id);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public SaleResponseDTO cancelSale(@PathVariable Integer id) {
        return saleService.cancelSale(id);
    }
}