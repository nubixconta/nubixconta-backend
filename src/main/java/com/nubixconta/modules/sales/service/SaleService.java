package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.inventory.service.InventoryService;
import com.nubixconta.modules.sales.dto.customer.CustomerResponseDTO;
import com.nubixconta.modules.sales.dto.sales.SaleForAccountsReceivableDTO;
import com.nubixconta.modules.sales.entity.Customer;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.repository.SaleRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.service.ProductService;
import com.nubixconta.modules.sales.dto.sales.*;
import com.nubixconta.modules.sales.entity.SaleDetail;
import org.modelmapper.ModelMapper;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final CustomerService customerService;
    private final ProductService productService;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;

    /**
     * Retorna todas las ventas existentes como DTO de respuesta.
     */
    public List<SaleResponseDTO> findAll() {
        return saleRepository.findAll().stream()
                .map(sale -> modelMapper.map(sale, SaleResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Busca una venta por su ID y la retorna como DTO. Lanza NotFoundException si no existe.
     */
    public SaleResponseDTO findById(Integer id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Venta con ID " + id + " no encontrada"));
        return modelMapper.map(sale, SaleResponseDTO.class);
    }

    /**
     * Crea una nueva venta a partir de un DTO. Válida existencia de cliente y productos/servicios.
     */
    @Transactional
    public SaleResponseDTO createSale(SaleCreateDTO dto) {
        // Validación de unicidad de número de documento
        if (saleRepository.existsByDocumentNumber(dto.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe una venta con el número de documento: " + dto.getDocumentNumber());
        }
        // --- VALIDACIÓN DE DUPLICADOS EN DTO (Buena práctica, la mantenemos) ---
        if (dto.getSaleDetails() != null) {
            Set<Integer> seenProductIds = new HashSet<>();
            Set<String> seenServiceNames = new HashSet<>();

            for (SaleDetailCreateDTO detail : dto.getSaleDetails()) {
                if (detail.getProductId() != null) {
                    if (!seenProductIds.add(detail.getProductId())) {
                        throw new BusinessRuleException("Producto duplicado en los detalles: ID " + detail.getProductId());
                    }
                } else if (detail.getServiceName() != null && !detail.getServiceName().isBlank()) {
                    String normalizedService = detail.getServiceName().trim().toLowerCase();
                    if (!seenServiceNames.add(normalizedService)) {
                        throw new BusinessRuleException("Servicio duplicado en los detalles: " + detail.getServiceName());
                    }
                }
            }
        }

        // --- CONSTRUCCIÓN MANUAL ---

        // 1. Buscar entidades dependientes
        Customer customer = customerService.findEntityById(dto.getClientId());

        // 2. Crear la entidad Venta y poblarla MANUALMENTE desde el DTO
        Sale newSale = new Sale();
        newSale.setCustomer(customer);
        newSale.setDocumentNumber(dto.getDocumentNumber());
        newSale.setSaleStatus("PENDIENTE");
        newSale.setIssueDate(dto.getIssueDate());
        newSale.setSaleType(dto.getSaleType());
        newSale.setTotalAmount(dto.getTotalAmount());
        newSale.setSaleDescription(dto.getSaleDescription());
        newSale.setModuleType(dto.getModuleType());
        // ¡Importante! La colección de detalles empieza vacía.
        newSale.setSaleDetails(new HashSet<>());

        // NO HAY SAVE AQUÍ TODAVÍA

        // 3. Crear y asociar los detalles en un bucle
        if (dto.getSaleDetails() != null) {
            for (SaleDetailCreateDTO detailDTO : dto.getSaleDetails()) {
                // Validar que el detalle sea válido (producto o servicio, no ambos)
                boolean hasProduct = detailDTO.getProductId() != null;
                boolean hasService = detailDTO.getServiceName() != null && !detailDTO.getServiceName().isBlank();
                if (hasProduct == hasService) {
                    throw new BusinessRuleException("Detalle inválido: debe tener 'productId' o 'serviceName'.");
                }

                // Crear la entidad Detalle y poblarla MANUALMENTE
                SaleDetail newDetail = new SaleDetail();
                newDetail.setQuantity(detailDTO.getQuantity());
                newDetail.setUnitPrice(detailDTO.getUnitPrice());
                newDetail.setSubtotal(detailDTO.getSubtotal());

                if (hasProduct) {
                    Product product = productService.findEntityById(detailDTO.getProductId());
                    newDetail.setProduct(product);
                } else {
                    newDetail.setServiceName(detailDTO.getServiceName());
                }

                // --- LA CLAVE DE LA SINCRONIZACIÓN BIDIRECCIONAL ---
                // Usamos un método helper en la entidad Sale para añadir el detalle.
                // Esto asegura que ambas partes de la relación se establecen a la vez.
                newSale.addDetail(newDetail);
            }
        }

        // 4. Ahora que el grafo de objetos está completamente y correctamente construido en memoria,
        // lo persistimos TODO de una sola vez.
        Sale savedSale = saleRepository.save(newSale);

        // 5. Devolvemos el DTO de respuesta
        return modelMapper.map(savedSale, SaleResponseDTO.class);
    }

    /**
     * Retorna una lista de ventas emitidas dentro del rango de fechas proporcionado.
     */
    public List<SaleResponseDTO> findByIssueDateBetween(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        return saleRepository.findByIssueDateBetween(startDateTime, endDateTime)
                .stream()
                .map(sale -> modelMapper.map(sale, SaleResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Elimina una venta por ID. Lanza NotFoundException si no existe.
     */
    public void delete(Integer id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Venta con ID " + id + " no encontrada para eliminar."));

        // ✅ REGLA DE NEGOCIO: Solo se pueden eliminar ventas PENDIENTES.
        if (!"PENDIENTE".equals(sale.getSaleStatus())) {
            throw new BusinessRuleException("Solo se pueden eliminar ventas con estado PENDIENTE. Estado actual: " + sale.getSaleStatus());
        }
        saleRepository.deleteById(id);
    }

    //metodo para actualizar una venta con sus detalles, se rige con los campos del dtoUpdate
    @Transactional
    public SaleResponseDTO updateSalePartial(Integer id, SaleUpdateDTO dto) {
        // 1. Buscar la venta que vamos a actualizar
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Venta con ID " + id + " no encontrada"));

        // 2. Validar unicidad del número de documento (esta parte está perfecta)
        if (dto.getDocumentNumber() != null &&
                !dto.getDocumentNumber().equals(sale.getDocumentNumber()) &&
                saleRepository.existsByDocumentNumber(dto.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe otra venta con el número de documento: " + dto.getDocumentNumber());
        }
        // REGLA DE NEGOCIO: Solo se pueden editar ventas PENDIENTES.
        if (!"PENDIENTE".equals(sale.getSaleStatus())) {
            throw new BusinessRuleException("Solo se pueden editar ventas con estado PENDIENTE. Estado actual: " + sale.getSaleStatus());
        }

        // 3. Actualizar campos simples de la venta MANUALMENTE
        // Esto es más seguro que un mapeo general.
        if (dto.getDocumentNumber() != null) sale.setDocumentNumber(dto.getDocumentNumber());
        if (dto.getIssueDate() != null) sale.setIssueDate(dto.getIssueDate());
        if (dto.getSaleType() != null) sale.setSaleType(dto.getSaleType());
        if (dto.getTotalAmount() != null) sale.setTotalAmount(dto.getTotalAmount());
        if (dto.getSaleDescription() != null) sale.setSaleDescription(dto.getSaleDescription());

        // 4. Lógica de sincronización de detalles (si se proporcionan)
        if (dto.getSaleDetails() != null) {
            // Tu lógica de validación de duplicados y mapa es excelente... la mantenemos.
            Map<Object, SaleDetail> existingDetailsMap = sale.getSaleDetails().stream()
                    .collect(Collectors.toMap(
                            detail -> detail.getProduct() != null ? (Object) detail.getProduct().getIdProduct() : detail.getServiceName(),
                            Function.identity()
                    ));
            Set<SaleDetail> updatedDetails = new HashSet<>();

            for (SaleDetailCreateDTO detailDTO : dto.getSaleDetails()) {
                boolean hasProduct = detailDTO.getProductId() != null;
                Object key = hasProduct ? detailDTO.getProductId() : detailDTO.getServiceName();
                SaleDetail existingDetail = existingDetailsMap.get(key);

                if (existingDetail != null) {
                    // DETALLE EXISTENTE: Actualización 100% manual, sin ModelMapper.
                    existingDetail.setQuantity(detailDTO.getQuantity());
                    existingDetail.setUnitPrice(detailDTO.getUnitPrice());
                    existingDetail.setSubtotal(detailDTO.getSubtotal());
                    // No tocamos la relación 'sale' para evitar el error.

                    updatedDetails.add(existingDetail);
                    existingDetailsMap.remove(key);
                } else {
                    // NUEVO DETALLE: Usar el método helper que ya es seguro.
                    SaleDetail newDetail = mapToSaleDetail(detailDTO, sale);
                    updatedDetails.add(newDetail);
                }
            }

            // Reemplazar la colección para que Hibernate calcule los cambios.
            sale.getSaleDetails().clear();
            sale.getSaleDetails().addAll(updatedDetails);
        }

        // 5. Guardar la venta actualizada.
        Sale updatedSale = saleRepository.save(sale);
        return modelMapper.map(updatedSale, SaleResponseDTO.class);
    }

    // --- NUEVOS MÉTODOS PARA EL CICLO DE VIDA ---

    @Transactional
    public SaleResponseDTO applySale(Integer saleId) {
        // 1. Buscar la venta y sus detalles
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Venta con ID " + saleId + " no encontrada"));

        // 2. Validar estado actual
        if (!"PENDIENTE".equals(sale.getSaleStatus())) {
            throw new BusinessRuleException("La venta solo puede ser aplicada si su estado es PENDIENTE. Estado actual: " + sale.getSaleStatus());
        }

        // 3. Delegar la lógica de inventario al InventoryService
        inventoryService.processSaleApplication(sale);

        // 4. (Futuro) Aquí llamarías al servicio de contabilidad
        // accountingService.createAccountingEntryForSale(sale);

        // 5. Actualizar el estado de la venta
        sale.setSaleStatus("APLICADA");
        Sale appliedSale = saleRepository.save(sale);

        return modelMapper.map(appliedSale, SaleResponseDTO.class);
    }

    @Transactional
    public SaleResponseDTO cancelSale(Integer saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Venta con ID " + saleId + " no encontrada"));

        // 2. Validar estado actual
        if (!"APLICADA".equals(sale.getSaleStatus())) {
            throw new BusinessRuleException("La venta solo puede ser anulada si su estado es APLICADA. Estado actual: " + sale.getSaleStatus());
        }

        // 3. Delegar la REVERSIÓN de inventario al InventoryService
        inventoryService.processSaleCancellation(sale);

        // 4. (Futuro) Revertir la partida contable
        // accountingService.revertAccountingEntryForSale(sale);

        // 5. Actualizar estado
        sale.setSaleStatus("ANULADA");
        Sale cancelledSale = saleRepository.save(sale);

        return modelMapper.map(cancelledSale, SaleResponseDTO.class);
    }

    /**
     * Mapea un SaleDetailCreateDTO a una entidad SaleDetail, asociando el producto si es necesario.
     * Valida que tenga solo producto o servicio, pero no ambos o ninguno.
     */
    private SaleDetail mapToSaleDetail(SaleDetailCreateDTO dto, Sale sale) {
        boolean hasProduct = dto.getProductId() != null;
        boolean hasService = dto.getServiceName() != null && !dto.getServiceName().isBlank();

        if (hasProduct == hasService) {
            throw new BusinessRuleException("Cada detalle debe tener solo 'productId' o 'serviceName', no ambos o ninguno.");
        }

        SaleDetail detail = modelMapper.map(dto, SaleDetail.class);
        // 2. ¡ASEGURA QUE SEA UNA NUEVA ENTIDAD!
        // Esta es la línea más importante. Le decimos explícitamente a Hibernate:
        // "Este objeto es nuevo, ignora cualquier ID que ModelMapper haya podido asignar".
        detail.setSaleDetailId(null);

        detail.setSale(sale); // Asociar la venta padre

        // Si es producto, buscar y asociar la entidad real (no un DTO)
        if (hasProduct) {
            Product product = productService.findEntityById(dto.getProductId());
            detail.setProduct(product);
        }

        return detail;
    }
    public List<SaleResponseDTO> findByCustomerSearch(
            String name, String lastName, String dui, String nit
    ) {
        List<CustomerResponseDTO> customers = customerService.searchActive(name, lastName, dui, nit);
        if (customers.isEmpty()) {
            return List.of();
        }
        List<Integer> customerIds = customers.stream()
                .map(CustomerResponseDTO::getClientId) // <-- Cambiado aquí
                .toList();
        return saleRepository.findByCustomerIds(customerIds).stream()
                .map(sale -> modelMapper.map(sale, SaleResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<SaleForAccountsReceivableDTO> findSalesForAccountsReceivable() {
        return saleRepository.findAll().stream()
                .map(sale -> {
                    SaleForAccountsReceivableDTO dto = new SaleForAccountsReceivableDTO();
                    dto.setDocumentNumber(sale.getDocumentNumber());
                    dto.setTotalAmount(sale.getTotalAmount());
                    dto.setIssueDate(sale.getIssueDate());
                    dto.setCustomerName(sale.getCustomer().getCustomerName());
                    dto.setCustomerLastName(sale.getCustomer().getCustomerLastName());
                    dto.setCreditDay(sale.getCustomer().getCreditDay());
                    return dto;
                })
                .collect(Collectors.toList());
    }

}
