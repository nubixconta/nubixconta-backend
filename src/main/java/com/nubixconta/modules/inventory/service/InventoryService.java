package com.nubixconta.modules.inventory.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import com.nubixconta.modules.inventory.dto.movement.ManualMovementCreateDTO;
import com.nubixconta.modules.inventory.dto.movement.ManualMovementUpdateDTO;
import com.nubixconta.modules.inventory.dto.movement.MovementResponseDTO;
import com.nubixconta.modules.inventory.entity.InventoryMovement;
import com.nubixconta.modules.inventory.entity.MovementStatus; // Asegúrate que importa el Enum correcto
import com.nubixconta.modules.inventory.entity.MovementType;   // Asegúrate que importa el Enum correcto
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
import com.nubixconta.security.TenantContext;
import org.springframework.stereotype.Service;
import com.nubixconta.modules.administration.entity.Company;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final ChangeHistoryService changeHistoryService;

    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

    /**
     * Procesa la afectación de inventario cuando una VENTA es APLICADA.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processSaleApplication(Sale sale) {
        for (SaleDetail detail : sale.getSaleDetails()) {
            if (detail.getProduct() != null) {
                Product product = productRepository.findById(detail.getProduct().getIdProduct())
                        .orElseThrow(() -> new BusinessRuleException("Producto con ID " + detail.getProduct().getIdProduct() + " no encontrado durante la aplicación de la venta."));

                Integer quantityToDecrease = detail.getQuantity();

                if (product.getStockQuantity() < quantityToDecrease) {
                    throw new BusinessRuleException(
                            "Stock insuficiente para aplicar la venta. Producto: " + product.getProductName() +
                                    ". Stock actual: " + product.getStockQuantity() + ", se requiere: " + quantityToDecrease
                    );
                }

                int newStock = product.getStockQuantity() - quantityToDecrease;
                product.setStockQuantity(newStock);
                productRepository.save(product);

                createMovementRecord(product, quantityToDecrease, MovementType.SALIDA, newStock, sale, null,sale.getCompany());
            }
        }
    }

    /**
     * Procesa la reversión de inventario cuando una VENTA es ANULADA.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processSaleCancellation(Sale sale) {
        for (SaleDetail detail : sale.getSaleDetails()) {
            if (detail.getProduct() != null) {
                Product product = productRepository.findById(detail.getProduct().getIdProduct())
                        .orElseThrow(() -> new BusinessRuleException("Producto con ID " + detail.getProduct().getIdProduct() + " no encontrado durante la anulación de la venta."));

                int newStock = product.getStockQuantity() + detail.getQuantity();
                product.setStockQuantity(newStock);
                productRepository.save(product);

                createMovementRecord(product, detail.getQuantity(), MovementType.ENTRADA, newStock, sale, null,sale.getCompany());
            }
        }
    }

    /**
     * Procesa la afectación de inventario cuando una NOTA DE CRÉDITO es APLICADA.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processCreditNoteApplication(CreditNote creditNote) {
        for (CreditNoteDetail detail : creditNote.getDetails()) {
            if (detail.getProduct() != null) {
                Product product = productRepository.findById(detail.getProduct().getIdProduct())
                        .orElseThrow(() -> new BusinessRuleException("Producto con ID " + detail.getProduct().getIdProduct() + " no encontrado durante la aplicación de la nota de crédito."));

                int newStock = product.getStockQuantity() + detail.getQuantity();
                product.setStockQuantity(newStock);
                productRepository.save(product);

                createMovementRecord(product, detail.getQuantity(), MovementType.ENTRADA, newStock, null, creditNote,creditNote.getCompany());
            }
        }
    }

    /**
     * Procesa la reversión de inventario cuando una NOTA DE CRÉDITO es ANULADA.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processCreditNoteCancellation(CreditNote creditNote) {
        for (CreditNoteDetail detail : creditNote.getDetails()) {
            if (detail.getProduct() != null) {
                Product product = productRepository.findById(detail.getProduct().getIdProduct())
                        .orElseThrow(() -> new BusinessRuleException("Producto con ID " + detail.getProduct().getIdProduct() + " no encontrado durante la anulación de la nota de crédito."));

                int quantityToDecrease = detail.getQuantity();
                if (product.getStockQuantity() < quantityToDecrease) {
                    throw new BusinessRuleException("Stock insuficiente para anular la nota de crédito del producto '" + product.getProductName() + "'. El stock se ha movido por otra operación.");
                }
                int newStock = product.getStockQuantity() - quantityToDecrease;
                product.setStockQuantity(newStock);
                productRepository.save(product);

                createMovementRecord(product, quantityToDecrease, MovementType.SALIDA, newStock, null, creditNote,creditNote.getCompany());
            }
        }
    }

    /**
     * Crea un nuevo registro de movimiento manual en estado PENDIENTE.
     */
    @Transactional
    public MovementResponseDTO createManualMovement(ManualMovementCreateDTO dto) {
        Integer companyId = getCompanyIdFromContext();
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundException("Producto con ID " + dto.getProductId() + " no encontrado."));

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setQuantity(dto.getQuantity());
        movement.setMovementType(dto.getMovementType());
        movement.setDescription(dto.getDescription());
        movement.setStatus(MovementStatus.PENDIENTE);
        movement.setStockAfterMovement(product.getStockQuantity());
        movement.setCompany(product.getCompany());

        InventoryMovement savedMovement = movementRepository.save(movement);
        // --- INICIO: REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Creó un ajuste PENDIENTE de %s de %d unidad(es) para el producto '%s'. Motivo: %s",
                savedMovement.getMovementType(),
                savedMovement.getQuantity(),
                savedMovement.getProduct().getProductName(),
                savedMovement.getDescription());
        changeHistoryService.logChange("Inventario - Movimientos", logMessage);
        // --- FIN: REGISTRO EN BITÁCORA ---
        return MovementResponseDTO.fromEntity(savedMovement);
    }

    /**
     * Devuelve una lista de todos los movimientos de inventario, con ordenamiento personalizable.
     * @param sortBy Criterio de ordenamiento: "status" (default) o "date".
     * @return Lista de MovementResponseDTO ordenados.
     */
    @Transactional(readOnly = true)
    public List<MovementResponseDTO> findAllMovements(String sortBy) { // Acepta el nuevo parámetro
        Integer companyId = getCompanyIdFromContext();
        List<InventoryMovement> movements;

        // Lógica de selección de ordenamiento, idéntica a tu servicio de Ventas.
        if ("status".equalsIgnoreCase(sortBy)) {
            movements = movementRepository.findAllByCompanyIdOrderByStatusAndDateWithDetails(companyId);
        } else {
            // "date" o cualquier otro valor (o nulo) será el fallback seguro.
            movements = movementRepository.findByCompanyIdOrderByDateDescWithDetails(companyId);
        }

        return movements.stream()
                .map(MovementResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve una lista de movimientos filtrada por un rango de fechas.
     */
    @Transactional(readOnly = true)
    public List<MovementResponseDTO> findMovementsByDateRange(LocalDate startDate, LocalDate endDate) {
        Integer companyId = getCompanyIdFromContext();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        // --- CAMBIO: Se llama al nuevo método con 'WithDetails' ---
        List<InventoryMovement> movements = movementRepository.findByCompanyIdAndDateBetweenWithDetails(companyId, startDateTime, endDateTime);

        return movements.stream()
                .map(MovementResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Aplica un movimiento manual PENDIENTE, afectando el stock del producto.
     */
    @Transactional
    public MovementResponseDTO applyManualMovement(Integer movementId) {
        InventoryMovement movement = findAndValidateManualMovement(movementId);

        if (movement.getStatus() != MovementStatus.PENDIENTE) {
            throw new BusinessRuleException("Solo se pueden aplicar movimientos en estado PENDIENTE. Estado actual: " + movement.getStatus());
        }

        Product product = movement.getProduct();
        int newStock;

        if (movement.getMovementType() == MovementType.SALIDA) {
            if (product.getStockQuantity() < movement.getQuantity()) {
                throw new BusinessRuleException("Stock insuficiente para el producto '" + product.getProductName() + "'. Stock actual: " + product.getStockQuantity() + ", se requiere: " + movement.getQuantity());
            }
            newStock = product.getStockQuantity() - movement.getQuantity();
        } else {
            newStock = product.getStockQuantity() + movement.getQuantity();
        }

        product.setStockQuantity(newStock);
        productRepository.save(product);

        movement.setStatus(MovementStatus.APLICADA);
        movement.setStockAfterMovement(newStock);
        InventoryMovement appliedMovement = movementRepository.save(movement);

        // --- INICIO: REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Aplicó el ajuste N° %d (%s de %d unidad(es) para '%s'). Estado cambió a APLICADO.",
                appliedMovement.getMovementId(),
                appliedMovement.getMovementType(),
                appliedMovement.getQuantity(),
                appliedMovement.getProduct().getProductName());
        changeHistoryService.logChange("Inventario - Movimientos", logMessage);
        // --- FIN: REGISTRO EN BITÁCORA ---

        return MovementResponseDTO.fromEntity(appliedMovement);
    }

    /**
     * Anula un movimiento manual APLICADO, revirtiendo la afectación al stock.
     */
    @Transactional
    public MovementResponseDTO cancelManualMovement(Integer movementId) {
        InventoryMovement movement = findAndValidateManualMovement(movementId);

        if (movement.getStatus() != MovementStatus.APLICADA) {
            throw new BusinessRuleException("Solo se pueden anular movimientos en estado APLICADO. Estado actual: " + movement.getStatus());
        }

        Product product = movement.getProduct();
        int newStock;

        if (movement.getMovementType() == MovementType.SALIDA) {
            newStock = product.getStockQuantity() + movement.getQuantity();
        } else {
            if (product.getStockQuantity() < movement.getQuantity()) {
                throw new BusinessRuleException("Stock insuficiente para anular la entrada del producto '" + product.getProductName() + "'. Stock actual: " + product.getStockQuantity() + ", se requiere revertir: " + movement.getQuantity());
            }
            newStock = product.getStockQuantity() - movement.getQuantity();
        }

        product.setStockQuantity(newStock);
        productRepository.save(product);

        movement.setStatus(MovementStatus.ANULADA);
        movement.setStockAfterMovement(newStock);
        InventoryMovement cancelledMovement = movementRepository.save(movement);

        // --- INICIO: REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Anuló el ajuste N° %d para el producto '%s'. Estado cambió a ANULADO.",
                cancelledMovement.getMovementId(),
                cancelledMovement.getProduct().getProductName());
        changeHistoryService.logChange("Inventario - Movimientos", logMessage);
        // --- FIN: REGISTRO EN BITÁCORA ---

        return MovementResponseDTO.fromEntity(cancelledMovement);
    }

    /**
     * Actualiza parcialmente los datos de un movimiento manual que está en estado PENDIENTE.
     */
    @Transactional
    public MovementResponseDTO updateManualMovement(Integer movementId, ManualMovementUpdateDTO dto) {
        InventoryMovement movement = findAndValidateManualMovement(movementId);

        if (movement.getStatus() != MovementStatus.PENDIENTE) {
            throw new BusinessRuleException("Solo se pueden editar movimientos en estado PENDIENTE.");
        }

        if (dto.getProductId() != null) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new NotFoundException("Producto con ID " + dto.getProductId() + " no encontrado para la actualización."));
            movement.setProduct(product);
        }
        if (dto.getQuantity() != null) {
            if (dto.getQuantity() <= 0) {
                throw new BusinessRuleException("La cantidad debe ser un número positivo.");
            }
            movement.setQuantity(dto.getQuantity());
        }
        if (dto.getMovementType() != null) {
            movement.setMovementType(dto.getMovementType());
        }
        if (dto.getDescription() != null) {
            if (dto.getDescription().isBlank()) {
                throw new BusinessRuleException("La descripción no puede estar vacía.");
            }
            movement.setDescription(dto.getDescription());
        }

        InventoryMovement updatedMovement = movementRepository.save(movement);
        // --- INICIO: REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Actualizó el ajuste PENDIENTE N° %d para el producto '%s'.",
                updatedMovement.getMovementId(),
                updatedMovement.getProduct().getProductName());
        changeHistoryService.logChange("Inventario - Movimientos", logMessage);
        // --- FIN: REGISTRO EN BITÁCORA ---
        return MovementResponseDTO.fromEntity(updatedMovement);
    }

    /**
     * Elimina un movimiento manual que está en estado PENDIENTE.
     */
    @Transactional
    public void deleteManualMovement(Integer movementId) {
        InventoryMovement movement = findAndValidateManualMovement(movementId);

        if (movement.getStatus() != MovementStatus.PENDIENTE) {
            throw new BusinessRuleException("Solo se pueden eliminar movimientos en estado PENDIENTE.");
        }
        // --- INICIO: REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Eliminó el ajuste PENDIENTE N° %d para el producto '%s'.",
                movementId,
                movement.getProduct().getProductName());
        changeHistoryService.logChange("Inventario - Movimientos", logMessage);
        // --- FIN: REGISTRO EN BITÁCORA ---
        movementRepository.delete(movement);
    }

    /**
     * Método de ayuda privado para buscar un movimiento y validar que es manual.
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
     */
    private void createMovementRecord(Product product, Integer quantity, MovementType type, Integer stockAfter, Sale sale, CreditNote creditNote, Company company) {
        InventoryMovement movement = new InventoryMovement();
        movement.setCompany(company);
        movement.setProduct(product);
        movement.setQuantity(quantity);
        movement.setMovementType(type);
        movement.setStockAfterMovement(stockAfter);
        movement.setStatus(MovementStatus.APLICADA);

        if (sale != null) {
            movement.setSale(sale);
            String action = type == MovementType.SALIDA ? "Salida por Venta" : "Reversión por Anulación de Venta";
            movement.setDescription(action + ": " + sale.getDocumentNumber());
        } else if (creditNote != null) {
            movement.setCreditNote(creditNote);
            String action = type == MovementType.ENTRADA ? "Entrada por Nota de Crédito" : "Reversión por Anulación de N.C.";
            movement.setDescription(action + ": " + creditNote.getDocumentNumber());
        }
        movementRepository.save(movement);
    }
}