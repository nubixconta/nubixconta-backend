package com.nubixconta.modules.inventory.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.inventory.dto.movement.ManualMovementCreateDTO;
import com.nubixconta.modules.inventory.dto.movement.ManualMovementUpdateDTO;
import com.nubixconta.modules.inventory.dto.movement.MovementResponseDTO;
import com.nubixconta.modules.inventory.entity.InventoryMovement;
import com.nubixconta.modules.inventory.entity.MovementStatus;
import com.nubixconta.modules.inventory.entity.MovementType;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.repository.InventoryMovementRepository;
import com.nubixconta.modules.inventory.repository.ProductRepository;
import com.nubixconta.modules.sales.entity.CreditNote;
import com.nubixconta.modules.sales.entity.CreditNoteDetail;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.entity.SaleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMovementRepository movementRepository;
    // Asumo que tienes un ProductRepository, si no, créalo.
    private final ProductRepository productRepository;

    /**
     * Procesa la afectación de inventario cuando una VENTA es APLICADA.
     * Es llamado por SaleService.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processSaleApplication(Sale sale) {
        for (SaleDetail detail : sale.getSaleDetails()) {
            // Solo procesamos detalles que son productos y afectan inventario
            if (detail.getProduct() != null) {
                Product product = productRepository.findById(detail.getProduct().getIdProduct())
                        .orElseThrow(() -> new BusinessRuleException("Producto con ID " + detail.getProduct().getIdProduct() + " no encontrado durante la aplicación de la venta."));

                Integer quantityToDecrease = detail.getQuantity();

                // 1. Validar Stock
                if (product.getStockQuantity() < quantityToDecrease) {
                    throw new BusinessRuleException(
                            "Stock insuficiente para aplicar la venta. Producto: " + product.getProductName() +
                                    ". Stock actual: " + product.getStockQuantity() + ", se requiere: " + quantityToDecrease
                    );
                }

                // 2. Actualizar Stock
                int newStock = product.getStockQuantity() - quantityToDecrease;
                product.setStockQuantity(newStock);
                productRepository.save(product);

                // 3. Crear el registro del movimiento de SALIDA
                createMovementRecord(product, quantityToDecrease, MovementType.OUT, newStock, sale, null);
            }
        }
    }

    /**
     * Procesa la reversión de inventario cuando una VENTA es ANULADA.
     * Es llamado por SaleService.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processSaleCancellation(Sale sale) {
        for (SaleDetail detail : sale.getSaleDetails()) {
            // Solo revertimos stock para detalles que son productos
            if (detail.getProduct() != null) {
                Product product = productRepository.findById(detail.getProduct().getIdProduct())
                        .orElseThrow(() -> new BusinessRuleException("Producto con ID " + detail.getProduct().getIdProduct() + " no encontrado durante la anulación de la venta."));

                Integer quantityToIncrease = detail.getQuantity();

                // 1. Devolver el stock al producto
                int newStock = product.getStockQuantity() + quantityToIncrease;
                product.setStockQuantity(newStock);
                productRepository.save(product);

                // ¡AQUÍ ESTÁ LA CLAVE! Creamos un movimiento de ENTRADA para registrar la anulación.
                createMovementRecord(
                        product,
                        detail.getQuantity(),
                        MovementType.IN, // El stock ENTRA de vuelta
                        newStock,
                        sale,          // Parámetro 'sale'
                        null           // Parámetro 'creditNote'
                );
            }
        }
    }
    // --- ✅ NUEVA LÓGICA PARA NOTAS DE CRÉDITO ---

    /**
     * Procesa la afectación de inventario cuando una NOTA DE CRÉDITO es APLICADA.
     * Incrementa el stock porque el producto es devuelto.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processCreditNoteApplication(CreditNote creditNote) {
        for (CreditNoteDetail detail : creditNote.getDetails()) {
            if (detail.getProduct() != null) {
                Product product = productRepository.findById(detail.getProduct().getIdProduct())
                        .orElseThrow(() -> new BusinessRuleException("Producto con ID " + detail.getProduct().getIdProduct() + " no encontrado durante la aplicación de la nota de crédito."));

                // Lógica INVERSA a la venta: Aumentamos el stock.
                int newStock = product.getStockQuantity() + detail.getQuantity();
                product.setStockQuantity(newStock);
                productRepository.save(product);

                // Creamos un movimiento de ENTRADA.
                createMovementRecord(product, detail.getQuantity(), MovementType.IN, newStock, null, creditNote);
            }
        }
    }

    /**
     * Procesa la reversión de inventario cuando una NOTA DE CRÉDITO es ANULADA.
     * Reduce el stock, revirtiendo la devolución original.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processCreditNoteCancellation(CreditNote creditNote) {
        for (CreditNoteDetail detail : creditNote.getDetails()) {
            if (detail.getProduct() != null) {
                Product product = productRepository.findById(detail.getProduct().getIdProduct())
                        .orElseThrow(() -> new BusinessRuleException("Producto con ID " + detail.getProduct().getIdProduct() + " no encontrado durante la anulación de la nota de crédito."));

                // Lógica INVERSA: Revertimos la entrada, por lo tanto, reducimos el stock.
                int quantityToDecrease = detail.getQuantity();
                if (product.getStockQuantity() < quantityToDecrease) {
                    // Caso borde: si el stock ya no es suficiente para revertir, es un problema de integridad.
                    throw new BusinessRuleException("Stock insuficiente para anular la nota de crédito del producto '" + product.getProductName() + "'. El stock se ha movido por otra operación.");
                }
                int newStock = product.getStockQuantity() - quantityToDecrease;
                product.setStockQuantity(newStock);
                productRepository.save(product);

                // Creamos un movimiento de SALIDA para registrar la anulación.
                createMovementRecord(product, quantityToDecrease, MovementType.OUT, newStock, null, creditNote);
            }
        }
    }

    // --- LÓGICA PARA MOVIMIENTOS MANUALES ---

    // --- MÉTODOS PARA MOVIMIENTOS MANUALES ---

    /**
     * Crea un nuevo registro de movimiento manual en estado PENDIENTE.
     * NO afecta el stock todavía.
     */
    @Transactional
    public MovementResponseDTO createManualMovement(ManualMovementCreateDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundException("Producto con ID " + dto.getProductId() + " no encontrado."));

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setQuantity(dto.getQuantity());
        movement.setMovementType(dto.getMovementType());
        movement.setDescription(dto.getDescription());

        // El movimiento nace PENDIENTE y no afecta el stock.
        movement.setStatus(MovementStatus.PENDING);
        // Como no hay afectación, el "stock después" es simplemente el stock actual.
        // Este campo se actualizará correctamente cuando se aplique el movimiento.
        movement.setStockAfterMovement(product.getStockQuantity());

        InventoryMovement savedMovement = movementRepository.save(movement);
        return MovementResponseDTO.fromEntity(savedMovement);
    }
    /**
     * Devuelve una lista completa de todos los movimientos de inventario.
     */
    @Transactional(readOnly = true)
    public List<MovementResponseDTO> findAllMovements() {
        List<InventoryMovement> movements = movementRepository.findAll();
        return movements.stream()
                .map(MovementResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve una lista de movimientos filtrada por un rango de fechas.
     */
    @Transactional(readOnly = true)
    public List<MovementResponseDTO> findMovementsByDateRange(LocalDate startDate, LocalDate endDate) {
        // Convertimos las fechas (LocalDate) a fecha y hora (LocalDateTime) para
        // asegurarnos de que la búsqueda incluya todo el día.
        LocalDateTime startDateTime = startDate.atStartOfDay(); // Ej: 2025-07-12T00:00:00
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX); // Ej: 2025-07-12T23:59:59.999...

        List<InventoryMovement> movements = movementRepository.findByDateBetween(startDateTime, endDateTime);

        return movements.stream()
                .map(MovementResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
    /**
     * Aplica un movimiento manual PENDIENTE, afectando el stock del producto.
     */
    @Transactional
    public MovementResponseDTO applyManualMovement(Integer movementId) {
        InventoryMovement movement = movementRepository.findById(movementId)
                .orElseThrow(() -> new NotFoundException("Movimiento de inventario con ID " + movementId + " no encontrado."));

        // Validaciones de seguridad
        if (movement.getSale() != null || movement.getCreditNote() != null) {
            throw new BusinessRuleException("Este movimiento fue generado por otro módulo y no puede ser gestionado manualmente.");
        }
        if (movement.getStatus() != MovementStatus.PENDING) {
            throw new BusinessRuleException("Solo se pueden aplicar movimientos en estado PENDIENTE. Estado actual: " + movement.getStatus());
        }

        Product product = movement.getProduct();
        int newStock;

        if (movement.getMovementType() == MovementType.OUT) { // Salida de inventario
            if (product.getStockQuantity() < movement.getQuantity()) {
                throw new BusinessRuleException("Stock insuficiente para el producto '" + product.getProductName() + "'. Stock actual: " + product.getStockQuantity() + ", se requiere: " + movement.getQuantity());
            }
            newStock = product.getStockQuantity() - movement.getQuantity();
        } else { // Entrada de inventario
            newStock = product.getStockQuantity() + movement.getQuantity();
        }

        // Actualizar el stock del producto
        product.setStockQuantity(newStock);
        productRepository.save(product);

        // Actualizar el movimiento
        movement.setStatus(MovementStatus.APPLIED);
        movement.setStockAfterMovement(newStock);
        InventoryMovement appliedMovement = movementRepository.save(movement);

        return MovementResponseDTO.fromEntity(appliedMovement);
    }

    /**
     * Anula un movimiento manual APLICADO, revirtiendo la afectación al stock.
     */
    @Transactional
    public MovementResponseDTO cancelManualMovement(Integer movementId) {
        InventoryMovement movement = movementRepository.findById(movementId)
                .orElseThrow(() -> new NotFoundException("Movimiento de inventario con ID " + movementId + " no encontrado."));

        // Validaciones de seguridad
        if (movement.getSale() != null || movement.getCreditNote() != null) {
            throw new BusinessRuleException("Este movimiento fue generado por otro módulo y no puede ser gestionado manualmente.");
        }
        if (movement.getStatus() != MovementStatus.APPLIED) {
            throw new BusinessRuleException("Solo se pueden anular movimientos en estado APLICADO. Estado actual: " + movement.getStatus());
        }

        Product product = movement.getProduct();
        int newStock;

        // Lógica de REVERSIÓN: hacemos la operación contraria a la original
        if (movement.getMovementType() == MovementType.OUT) { // Si el original fue una SALIDA, ahora es una ENTRADA
            newStock = product.getStockQuantity() + movement.getQuantity();
        } else { // Si el original fue una ENTRADA, ahora es una SALIDA
            // Por seguridad, incluso en una reversión, validamos el stock
            if (product.getStockQuantity() < movement.getQuantity()) {
                throw new BusinessRuleException("Stock insuficiente para anular la entrada del producto '" + product.getProductName() + "'. Stock actual: " + product.getStockQuantity() + ", se requiere revertir: " + movement.getQuantity());
            }
            newStock = product.getStockQuantity() - movement.getQuantity();
        }

        // Actualizar el stock del producto
        product.setStockQuantity(newStock);
        productRepository.save(product);

        // Actualizar el estado del movimiento
        movement.setStatus(MovementStatus.CANCELLED);
        // Opcional: podrías actualizar el stockAfterMovement al nuevo valor, para tener un registro más claro.
        movement.setStockAfterMovement(newStock);
        InventoryMovement cancelledMovement = movementRepository.save(movement);

        return MovementResponseDTO.fromEntity(cancelledMovement);
    }
    /**
     * Actualiza parcialmente los datos de un movimiento manual que está en estado PENDIENTE.
     * Sigue la semántica de un PATCH: solo los campos presentes en el DTO son modificados.
     */
    @Transactional
    public MovementResponseDTO updateManualMovement(Integer movementId, ManualMovementUpdateDTO dto) {
        // 1. Buscar la entidad y validar que es un movimiento manual
        InventoryMovement movement = findAndValidateManualMovement(movementId);

        // 2. Validar la regla de negocio más importante: el estado.
        if (movement.getStatus() != MovementStatus.PENDING) {
            throw new BusinessRuleException("Solo se pueden editar movimientos en estado PENDIENTE.");
        }

        // --- LÓGICA DE ACTUALIZACIÓN PARCIAL ---
        // Aplicamos los cambios solo si el campo correspondiente vino en el DTO (no es nulo).

        if (dto.getProductId() != null) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new NotFoundException("Producto con ID " + dto.getProductId() + " no encontrado para la actualización."));
            movement.setProduct(product);
        }

        if (dto.getQuantity() != null) {
            // Podrías añadir validaciones aquí si es necesario (ej: > 0)
            if(dto.getQuantity() <= 0) {
                throw new BusinessRuleException("La cantidad debe ser un número positivo.");
            }
            movement.setQuantity(dto.getQuantity());
        }

        if (dto.getMovementType() != null) {
            movement.setMovementType(dto.getMovementType());
        }

        if (dto.getDescription() != null) {
            // Aquí podrías validar que no sea una cadena vacía si es una regla de negocio
            if(dto.getDescription().isBlank()) {
                throw new BusinessRuleException("La descripción no puede estar vacía.");
            }
            movement.setDescription(dto.getDescription());
        }

        // 3. Guardar la entidad actualizada
        InventoryMovement updatedMovement = movementRepository.save(movement);
        return MovementResponseDTO.fromEntity(updatedMovement);
    }

    /**
     * Elimina un movimiento manual que está en estado PENDIENTE.
     */
    @Transactional
    public void deleteManualMovement(Integer movementId) {
        InventoryMovement movement = findAndValidateManualMovement(movementId);

        // Validar que esté PENDIENTE
        if (movement.getStatus() != MovementStatus.PENDING) {
            throw new BusinessRuleException("Solo se pueden eliminar movimientos en estado PENDIENTE.");
        }

        movementRepository.delete(movement);
    }

    /**
     * Método de ayuda privado para buscar un movimiento y validar que es manual.
     * Evita la duplicación de código.
     */
    private InventoryMovement findAndValidateManualMovement(Integer movementId) {
        InventoryMovement movement = movementRepository.findById(movementId)
                .orElseThrow(() -> new NotFoundException("Movimiento de inventario con ID " + movementId + " no encontrado."));

        if (movement.getSale() != null || movement.getCreditNote() != null) {
            throw new BusinessRuleException("Este movimiento fue generado por otro módulo y no puede ser gestionado manualmente.");
        }
        return movement;
    }

    /**
     * Método helper privado para crear y guardar el registro de movimiento.
     * Ahora acepta una Venta O una Nota de Crédito.
     */
    private void createMovementRecord(Product product, Integer quantity, MovementType type, Integer stockAfter, Sale sale, CreditNote creditNote) {
        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setQuantity(quantity);
        movement.setMovementType(type);
        movement.setStockAfterMovement(stockAfter);
        movement.setStatus(MovementStatus.APPLIED); // Los movimientos de documentos siempre nacen aplicados

        // Lógica de descripción y enlace
        if (sale != null) {
            movement.setSale(sale);
            String action = type == MovementType.OUT ? "Salida por Venta" : "Reversión por Anulación de Venta";
            movement.setDescription(action + ": " + sale.getDocumentNumber());
        } else if (creditNote != null) {
            movement.setCreditNote(creditNote);
            String action = type == MovementType.IN ? "Entrada por Nota de Crédito" : "Reversión por Anulación de N.C.";
            movement.setDescription(action + ": " + creditNote.getDocumentNumber());
        }

        movementRepository.save(movement);
    }

}