package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.accounting.service.SalesAccountingService;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.inventory.service.InventoryService;
import com.nubixconta.modules.sales.dto.customer.CustomerResponseDTO;
import com.nubixconta.modules.sales.dto.sales.SaleForAccountsReceivableDTO;
import com.nubixconta.modules.sales.entity.Customer;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.repository.CustomerRepository;
import com.nubixconta.modules.sales.repository.SaleRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.security.TenantContext;

import java.math.BigDecimal;
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
    private final SalesAccountingService salesAccountingService;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;

    // Helper privado para obtener el contexto de la empresa de forma segura y consistente.
    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }


    /**
     * Retorna todas las ventas existentes, aplicando un ordenamiento específico.
     * @param sortBy El criterio de ordenamiento. "status" para agrupar por estado (default),
     *               "date" para ordenar solo por fecha.
     * @return Lista de SaleResponseDTO ordenadas.
     */
    public List<SaleResponseDTO> findAll(String sortBy) { // Ahora acepta un parámetro
        Integer companyId = getCompanyIdFromContext();
        List<Sale> sales;

        // "status" es el modo por defecto que agrupa.
        if ("status".equalsIgnoreCase(sortBy)) {
            sales =saleRepository.findAllByCompanyIdOrderByStatusAndIssueDate(companyId);
        }
        // "date" es el modo anterior que ordena solo por fecha.
        else if ("date".equalsIgnoreCase(sortBy)) {
            sales = saleRepository.findByCompany_IdOrderByIssueDateDesc(companyId);
        }
        // Por si envían otro valor, mantenemos el orden por fecha como un fallback seguro.
        else {
            sales = saleRepository.findByCompany_IdOrderByIssueDateDesc(companyId);
        }

        return sales.stream()
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
     * Busca las ventas de un cliente que son válidas para la creación de una nota de crédito.
     * Una venta es válida si está 'APLICADA' y no tiene una nota de crédito activa ('PENDIENTE' o 'APLICADA').
     *
     * @param clientId El ID del cliente.
     * @return Una lista de SaleResponseDTO con las ventas elegibles.
     */
    // Nombre del método cambiado de findAppliedSalesByClientId a uno más descriptivo.
    public List<SaleResponseDTO> findSalesAvailableForCreditNote(Integer clientId) {
        Integer companyId = getCompanyIdFromContext();
        customerService.findById(clientId);
        // Llamamos al nuevo método del repositorio que contiene toda la lógica.
        List<Sale> availableSales = saleRepository.findSalesAvailableForCreditNote(companyId, clientId);

        // El mapeo a DTO sigue siendo el mismo.
        return availableSales.stream()
                .map(sale -> modelMapper.map(sale, SaleResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<SaleResponseDTO> findByStatus(String status) {
        Integer companyId = getCompanyIdFromContext();
        List<Sale> sales = saleRepository.findByCompany_IdAndSaleStatus(companyId, status.toUpperCase());
        return sales.stream()
                .map(sale -> modelMapper.map(sale, SaleResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Busca ventas utilizando una combinación de filtros opcionales.
     * @param startDate Fecha de inicio opcional.
     * @param endDate Fecha de fin opcional.
     * @param customerName Nombre del cliente opcional.
     * @param customerLastName Apellido del cliente opcional.
     * @return Lista de SaleResponseDTO que coinciden con los filtros.
     */
    public List<SaleResponseDTO> findByCombinedCriteria(
            LocalDate startDate, LocalDate endDate, String customerName, String customerLastName
    ) {
        Integer companyId = getCompanyIdFromContext();
        // Convierte LocalDate a LocalDateTime para la consulta, manejando los límites del día.
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        List<Sale> sales = saleRepository.findByCombinedCriteria(companyId, startDateTime, endDateTime, customerName, customerLastName);

        return sales.stream()
                .map(sale -> modelMapper.map(sale, SaleResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Crea una nueva venta a partir de un DTO. Válida existencia de cliente y productos/servicios.
     */
    @Transactional
    public SaleResponseDTO createSale(SaleCreateDTO dto) {
        Integer companyId = getCompanyIdFromContext();
        // Validación de unicidad de número de documento
        if (saleRepository.existsByCompany_IdAndDocumentNumber(companyId, dto.getDocumentNumber())) {
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
        // --- INICIO DE LA NUEVA VALIDACIÓN DE INTEGRIDAD FINANCIERA ---

        BigDecimal calculatedSubtotalFromDetails = BigDecimal.ZERO;
        if (dto.getSaleDetails() != null) {
            for (SaleDetailCreateDTO detailDTO : dto.getSaleDetails()) {
                // 1. Validar la consistencia de cada línea de detalle
                BigDecimal lineSubtotal = detailDTO.getUnitPrice().multiply(BigDecimal.valueOf(detailDTO.getQuantity()));
                if (lineSubtotal.compareTo(detailDTO.getSubtotal()) != 0) {
                    throw new BusinessRuleException(
                            "Inconsistencia en el detalle del item '" + (detailDTO.getServiceName() != null ? detailDTO.getServiceName() : "Producto ID " + detailDTO.getProductId()) + "': " +
                                    "El subtotal enviado (" + detailDTO.getSubtotal() + ") no coincide con el cálculo (Precio " + detailDTO.getUnitPrice() + " * Cantidad " + detailDTO.getQuantity() + " = " + lineSubtotal + ")."
                    );
                }
                // 2. Sumar el subtotal (ya verificado) de la línea al total general
                calculatedSubtotalFromDetails = calculatedSubtotalFromDetails.add(detailDTO.getSubtotal());
            }
        }

        // 3. Validar que la suma de los detalles coincida con el subtotal de la cabecera
        if (calculatedSubtotalFromDetails.compareTo(dto.getSubtotalAmount()) != 0) {
            throw new BusinessRuleException(
                    "Inconsistencia en el subtotal de la venta: " +
                            "El subtotal enviado (" + dto.getSubtotalAmount() + ") no coincide con la suma de los subtotales de los detalles (" + calculatedSubtotalFromDetails + ")."
            );
        }

        // 4. Validar que Subtotal + IVA == Total (esta ya la tenías, la mantenemos como validación final)
        if (dto.getSubtotalAmount().add(dto.getVatAmount()).compareTo(dto.getTotalAmount()) != 0) {
            throw new BusinessRuleException("Inconsistencia en los totales: Subtotal + IVA no es igual al Total.");
        }

        // --- INICIO DE LA NUEVA UBICACIÓN PARA LA VALIDACIÓN DE CRÉDITO ---

        // 1. Obtenemos la entidad completa del cliente ANTES de construir la venta.
        Customer customer = customerService.findEntityById(dto.getClientId());

        // 2. Calculamos cuál sería el nuevo saldo si esta venta se aplicara.
        BigDecimal potentialNewBalance = customer.getCurrentBalance().add(dto.getTotalAmount());

        // 3. Comprobamos si el nuevo saldo excede el límite de crédito.
        if (potentialNewBalance.compareTo(customer.getCreditLimit()) > 0) {
            throw new BusinessRuleException(
                    "Límite de crédito excedido para el cliente. " +
                            "Límite: " + customer.getCreditLimit() + ", " +
                            "Saldo Actual: " + customer.getCurrentBalance() + ", " +
                            "Total de esta Venta: " + dto.getTotalAmount()
            );
        }
        // --- FIN DE LA NUEVA UBICACIÓN PARA LA VALIDACIÓN DE CRÉDITO ---

        // --- CONSTRUCCIÓN MANUAL ---


        //Obtener la referencia a la empresa.
        Company companyRef = companyRepository.getReferenceById(companyId);

        // 2. Crear la entidad Venta y poblarla MANUALMENTE desde el DTO
        Sale newSale = new Sale();
        newSale.setCustomer(customer);
        newSale.setCompany(companyRef);
        newSale.setDocumentNumber(dto.getDocumentNumber());
        newSale.setSaleStatus("PENDIENTE");
        newSale.setIssueDate(dto.getIssueDate());
        newSale.setSubtotalAmount(dto.getSubtotalAmount());
        newSale.setVatAmount(dto.getVatAmount());
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
        // 1. Obtener el contexto de la empresa.
        Integer companyId = getCompanyIdFromContext();
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        return saleRepository.findByCompany_IdAndIssueDateBetweenOrderByIssueDateDesc(companyId, startDateTime, endDateTime)
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

        //  REGLA DE NEGOCIO: Solo se pueden eliminar ventas PENDIENTES.
        if (!"PENDIENTE".equals(sale.getSaleStatus())) {
            throw new BusinessRuleException("Solo se pueden eliminar ventas con estado PENDIENTE. Estado actual: " + sale.getSaleStatus());
        }
        saleRepository.deleteById(id);
    }

    //metodo para actualizar una venta con sus detalles, se rige con los campos del dtoUpdate
    @Transactional
    public SaleResponseDTO updateSalePartial(Integer id, SaleUpdateDTO dto) {
        // 1. Obtener el contexto de la empresa.
        Integer companyId = getCompanyIdFromContext();

        // 2. Buscar la venta que vamos a actualizar
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Venta con ID " + id + " no encontrada"));

        // 3. Validar unicidad del número de documento (esta parte está perfecta)
        if (dto.getDocumentNumber() != null &&
                !dto.getDocumentNumber().equals(sale.getDocumentNumber()) &&
                saleRepository.existsByCompany_IdAndDocumentNumber(companyId, dto.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe otra venta con el número de documento: " + dto.getDocumentNumber());
        }
        //4. REGLA DE NEGOCIO: Solo se pueden editar ventas PENDIENTES.
        if (!"PENDIENTE".equals(sale.getSaleStatus())) {
            throw new BusinessRuleException("Solo se pueden editar ventas con estado PENDIENTE. Estado actual: " + sale.getSaleStatus());
        }
        // --- INICIO DE LA VALIDACIÓN FINANCIERA COMPLETA ---

        // 5. Si se envían detalles, se deben enviar también los totales y se validará todo.
        if (dto.getSaleDetails() != null) {
            if (dto.getSubtotalAmount() == null || dto.getVatAmount() == null || dto.getTotalAmount() == null) {
                throw new BusinessRuleException("Si se modifican los detalles, se deben enviar los nuevos valores de subtotalAmount, vatAmount y totalAmount.");
            }

            BigDecimal calculatedSubtotalFromDetails = BigDecimal.ZERO;
            for (SaleDetailCreateDTO detailDTO : dto.getSaleDetails()) {
                // 4.1. Validar la consistencia de cada línea de detalle
                BigDecimal lineSubtotal = detailDTO.getUnitPrice().multiply(BigDecimal.valueOf(detailDTO.getQuantity()));
                if (lineSubtotal.compareTo(detailDTO.getSubtotal()) != 0) {
                    throw new BusinessRuleException(
                            "Inconsistencia en el detalle del item '" + (detailDTO.getServiceName() != null ? detailDTO.getServiceName() : "Producto ID " + detailDTO.getProductId()) + "': " +
                                    "El subtotal enviado (" + detailDTO.getSubtotal() + ") no coincide con el cálculo (Precio " + detailDTO.getUnitPrice() + " * Cantidad " + detailDTO.getQuantity() + " = " + lineSubtotal + ")."
                    );
                }
                calculatedSubtotalFromDetails = calculatedSubtotalFromDetails.add(detailDTO.getSubtotal());
            }

            // 4.2. Validar que la suma de los detalles coincida con el subtotal de la cabecera
            if (calculatedSubtotalFromDetails.compareTo(dto.getSubtotalAmount()) != 0) {
                throw new BusinessRuleException(
                        "Inconsistencia en el subtotal de la venta: " +
                                "El subtotal enviado (" + dto.getSubtotalAmount() + ") no coincide con la suma de los subtotales de los detalles (" + calculatedSubtotalFromDetails + ")."
                );
            }
        }

        // 4.3. Validar que Subtotal + IVA == Total. Esta validación se ejecuta siempre,
        // incluso si solo se actualizan los totales sin cambiar los detalles.
        if (dto.getSubtotalAmount() != null && dto.getVatAmount() != null && dto.getTotalAmount() != null) {
            if (dto.getSubtotalAmount().add(dto.getVatAmount()).compareTo(dto.getTotalAmount()) != 0) {
                throw new BusinessRuleException("Inconsistencia en los totales: Subtotal + IVA no es igual al Total.");
            }
        }

        // --- FIN DE LA VALIDACIÓN FINANCIERA COMPLETA ---

        // 5. Actualizar campos simples de la venta MANUALMENTE
        // Esto es más seguro que un mapeo general.
        if (dto.getDocumentNumber() != null) sale.setDocumentNumber(dto.getDocumentNumber());
        if (dto.getIssueDate() != null) sale.setIssueDate(dto.getIssueDate());
        if (dto.getTotalAmount() != null) sale.setTotalAmount(dto.getTotalAmount());
        if (dto.getSaleDescription() != null) sale.setSaleDescription(dto.getSaleDescription());

        // Actualizar totales solo si se proporcionaron
        if(dto.getSubtotalAmount() != null) sale.setSubtotalAmount(dto.getSubtotalAmount());
        if(dto.getVatAmount() != null) sale.setVatAmount(dto.getVatAmount());
        if(dto.getTotalAmount() != null) sale.setTotalAmount(dto.getTotalAmount());

        // 6. Lógica de sincronización de detalles (si se proporcionan)
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

        // 7. Guardar la venta actualizada.
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
        // --- INICIO: NUEVA VALIDACIÓN DE PRODUCTOS ACTIVOS ---
        // 3. Antes de cualquier otra cosa, validamos el estado de los productos en el detalle.
        List<String> inactiveProductNames = sale.getSaleDetails().stream()
                // Filtramos solo los detalles que son productos
                .filter(detail -> detail.getProduct() != null)
                // Nos quedamos solo con los productos cuyo estado es 'false' (inactivo)
                .filter(detail -> !detail.getProduct().getProductStatus())
                // Mapeamos a los nombres de los productos para el mensaje de error
                .map(detail -> detail.getProduct().getProductName())
                // Recolectamos los nombres en una lista
                .collect(Collectors.toList());

        // 4. Si la lista de productos inactivos no está vacía, lanzamos un error.
        if (!inactiveProductNames.isEmpty()) {
            String errorDetails = String.join(", ", inactiveProductNames);
            throw new BusinessRuleException(
                    "No se puede aplicar la venta. Los siguientes productos están desactivados o eliminados: " + errorDetails +
                            ". Por favor, edite la venta para eliminar estos productos antes de aplicarla."
            );
        }
        // --- FIN: NUEVA VALIDACIÓN DE PRODUCTOS ACTIVOS ---

        // --- INICIO DE LA NUEVA LÓGICA DE VALIDACIÓN DE CRÉDITO ---

        Customer customer = sale.getCustomer();
        BigDecimal potentialNewBalance = customer.getCurrentBalance().add(sale.getTotalAmount());

        // 5. Comprobar si el nuevo saldo excede el límite de crédito
        if (potentialNewBalance.compareTo(customer.getCreditLimit()) > 0) {
            throw new BusinessRuleException(
                    "Límite de crédito excedido para el cliente. " +
                            "Límite: " + customer.getCreditLimit() + ", " +
                            "Saldo Actual: " + customer.getCurrentBalance() + ", " +
                            "Total de esta Venta: " + sale.getTotalAmount()
            );
        }

        // --- FIN DE LA NUEVA LÓGICA DE VALIDACIÓN DE CRÉDITO ---

        //    Esta será ahora la fecha oficial de la venta.
        sale.setIssueDate(LocalDateTime.now());
        
        // 6. Delegar la lógica de inventario al InventoryService
        inventoryService.processSaleApplication(sale);

        // 7. Generar asiento contable
        salesAccountingService.createEntriesForSaleApplication(sale);

        // 8. Actualizar el estado de la venta
        sale.setSaleStatus("APLICADA");
        customer.setCurrentBalance(potentialNewBalance); // <-- Actualizamos el saldo en la entidad

        // 9. Persistir todos los cambios (Venta y Cliente)
        customerRepository.save(customer); // <-- Guardamos el cliente con su nuevo saldo
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

        // 4. Revertir la partida contable
        salesAccountingService.deleteEntriesForSaleCancellation(sale);

        // --- NUEVA LÓGICA DE REVERSIÓN DE SALDO ---
        Customer customer = sale.getCustomer();
        BigDecimal newBalance = customer.getCurrentBalance().subtract(sale.getTotalAmount());
        customer.setCurrentBalance(newBalance);
        customerRepository.save(customer);
        // --- FIN ---

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
            // VALIDACIÓN ADICIONAL DE CONSISTENCIA: Asegurar que el producto pertenece a la misma empresa que la venta
            if (!product.getCompany().getId().equals(sale.getCompany().getId())) {
                throw new BusinessRuleException("Error de consistencia interna: El producto '" + product.getProductName() + "' no pertenece a la empresa de la venta.");
            }
            detail.setProduct(product);
        }

        return detail;
    }
    public List<SaleResponseDTO> findByCustomerSearch(
            String name, String lastName, String dui, String nit
    ) {
        // 1. Obtener el contexto de la empresa. Esto se usará en ambas búsquedas.
        Integer companyId = getCompanyIdFromContext();
        List<CustomerResponseDTO> customers = customerService.searchActive(name, lastName, dui, nit);
        if (customers.isEmpty()) {
            return List.of();
        }
        List<Integer> customerIds = customers.stream()
                .map(CustomerResponseDTO::getClientId) // <-- Cambiado aquí
                .toList();
        return saleRepository.findByCompanyIdAndCustomerIds(companyId,customerIds).stream()
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
