package com.nubixconta.modules.inventory.controller;

import com.nubixconta.modules.inventory.dto.movement.ManualMovementCreateDTO;
import com.nubixconta.modules.inventory.dto.movement.ManualMovementUpdateDTO;
import com.nubixconta.modules.inventory.dto.movement.MovementResponseDTO;
import com.nubixconta.modules.inventory.entity.InventoryMovement;
import com.nubixconta.modules.inventory.service.InventoryService;
import com.nubixconta.modules.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/v1/inventory-movements")
@RequiredArgsConstructor
public class InventoryMovementController {

    private final InventoryService inventoryService;

    /**
     * Endpoint para obtener la lista COMPLETA de todos los movimientos de inventario.
     */
    @GetMapping
    public ResponseEntity<List<MovementResponseDTO>> findAllMovements() {
        List<MovementResponseDTO> movements = inventoryService.findAllMovements();
        // Envolvemos la lista en un ResponseEntity con estado 200 OK.
        // El método .ok() es un atajo para ResponseEntity.status(HttpStatus.OK).
        return ResponseEntity.ok(movements);
    }

    /**
     * Endpoint para obtener una lista de movimientos filtrada por un rango de fechas.
     */
    @GetMapping("/by-date-range")
    public ResponseEntity<List<MovementResponseDTO>> findMovementsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        List<MovementResponseDTO> movements = inventoryService.findMovementsByDateRange(startDate, endDate);

        /*
           Ejemplo del poder de ResponseEntity:
           Si quisiéramos, podríamos devolver un código de estado diferente
           si la lista está vacía, lo cual es imposible con 'return List<DTO>'.

           if (movements.isEmpty()) {
               return ResponseEntity.noContent().build(); // Devuelve un 204 No Content
           }
        */

        return ResponseEntity.ok(movements);
    }

    // Endpoint para CREAR un nuevo ajuste manual (en estado PENDIENTE)
    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponseDTO createManualMovement(@Valid @RequestBody ManualMovementCreateDTO dto) {
        return inventoryService.createManualMovement(dto);
    }

    // Endpoint para APLICAR un ajuste manual que está PENDIENTE
    @PostMapping("/{id}/apply")
    public MovementResponseDTO applyManualMovement(@PathVariable Integer id) {
        return inventoryService.applyManualMovement(id);
    }

    // Endpoint para ANULAR un ajuste manual que ya fue APLICADO
    @PostMapping("/{id}/cancel")
    public MovementResponseDTO cancelManualMovement(@PathVariable Integer id) {
        return inventoryService.cancelManualMovement(id);
    }
    @PatchMapping("/manual/{id}")
    public MovementResponseDTO updateManualMovement(@PathVariable Integer id, @Valid @RequestBody ManualMovementUpdateDTO dto) {
        return inventoryService.updateManualMovement(id, dto);
    }

    @DeleteMapping("/manual/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content es la respuesta estándar para un DELETE exitoso
    public void deleteManualMovement(@PathVariable Integer id) {
        inventoryService.deleteManualMovement(id);
    }
}