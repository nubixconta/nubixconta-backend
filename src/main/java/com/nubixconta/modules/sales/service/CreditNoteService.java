package com.nubixconta.modules.sales.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.accounting.service.SalesAccountingService;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.service.InventoryService;
import com.nubixconta.modules.inventory.service.ProductService;
import com.nubixconta.modules.sales.dto.creditnote.*;
import com.nubixconta.modules.sales.entity.*;
import com.nubixconta.modules.sales.repository.CreditNoteRepository;
import com.nubixconta.modules.sales.repository.CustomerRepository;
import com.nubixconta.modules.sales.repository.SaleRepository;
import com.nubixconta.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final SaleRepository saleRepository; // Necesario para buscar la venta
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
     * Retorna todas las notas de crédito, aplicando un ordenamiento específico.
     * @param sortBy El criterio de ordenamiento. "status" para agrupar por estado (default),
     *               "date" para ordenar solo por fecha.
     * @return Lista de CreditNoteResponseDTO ordenadas.
     */
    public List<CreditNoteResponseDTO> findAll(String sortBy) {
        Integer companyId = getCompanyIdFromContext();
        List<CreditNote> creditNotes;
        if ("status".equalsIgnoreCase(sortBy)) {
            creditNotes = creditNoteRepository.findAllByCompanyIdOrderByStatusAndCreditNoteDate(companyId);
        } else {
            creditNotes = creditNoteRepository.findByCompany_IdOrderByIssueDateDesc(companyId);
        }
        return creditNotes.stream()
                .map(cn -> modelMapper.map(cn, CreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }


    /**
     * Busca una nota de crédito por su ID y la retorna como DTO.
     */
    public CreditNoteResponseDTO findById(Integer id) {
        CreditNote creditNote = creditNoteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nota de crédito con ID " + id + " no encontrada."));
        return modelMapper.map(creditNote, CreditNoteResponseDTO.class);
    }

    /**
     * Crea una nueva nota de crédito y sus detalles.
     */
    @Transactional
    public CreditNoteResponseDTO createCreditNote(CreditNoteCreateDTO dto) {
        Integer companyId = getCompanyIdFromContext();
        // 1. Validar unicidad del número de documento
        if (creditNoteRepository.existsByCompany_IdAndDocumentNumber(companyId, dto.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe una nota de crédito con el número de documento: " + dto.getDocumentNumber());
        }
        // 2. Buscar la venta asociada
        Sale sale = saleRepository.findById(dto.getSaleId())
                .orElseThrow(() -> new NotFoundException("La venta con ID " + dto.getSaleId() + " no fue encontrada."));
        // 3. VALIDACIÓN DE SEGURIDAD CRÍTICA: Asegurar que la venta pertenece a la empresa actual.
        if (!sale.getCompany().getId().equals(companyId)) {
            throw new BusinessRuleException("Acción no permitida: La venta asociada no pertenece a la empresa seleccionada.");
        }
        // 3. REGLA 1: Validar que la venta esté en estado APLICADA.
        if (!"APLICADA".equals(sale.getSaleStatus())) {
            throw new BusinessRuleException(
                    "No se puede crear una nota de crédito para una venta que no está en estado APLICADA. " +
                            "Estado actual de la venta: " + sale.getSaleStatus()
            );
        }

        // 4. REGLA 2: Validar que no exista ya una nota de crédito ACTIVA para esta venta.
        List<String> activeStatuses = List.of("PENDIENTE", "APLICADA");
        if (creditNoteRepository.existsByCompany_IdAndSale_SaleIdAndCreditNoteStatusIn(companyId, sale.getSaleId(), activeStatuses)) {
            throw new BusinessRuleException("Ya existe una nota de crédito activa (en estado PENDIENTE o APLICADA) para esta venta.");
        }

        // --- INICIO DE LA NUEVA VALIDACIÓN DE INTEGRIDAD FINANCIERA ---

        BigDecimal calculatedSubtotalFromDetails = BigDecimal.ZERO;
        if (dto.getDetails() != null) {
            for (CreditNoteDetailCreateDTO detailDTO : dto.getDetails()) {
                // 1. Validar la consistencia de cada línea de detalle
                BigDecimal lineSubtotal = detailDTO.getUnitPrice().multiply(BigDecimal.valueOf(detailDTO.getQuantity()));
                if (lineSubtotal.compareTo(detailDTO.getSubtotal()) != 0) {
                    throw new BusinessRuleException(
                            "Inconsistencia en el detalle de la nota de crédito para el item '" + (detailDTO.getServiceName() != null ? detailDTO.getServiceName() : "Producto ID " + detailDTO.getProductId()) + "': " +
                                    "El subtotal enviado (" + detailDTO.getSubtotal() + ") no coincide con el cálculo (Precio " + detailDTO.getUnitPrice() + " * Cantidad " + detailDTO.getQuantity() + " = " + lineSubtotal + ")."
                    );
                }
                // 2. Sumar el subtotal verificado de la línea al total general
                calculatedSubtotalFromDetails = calculatedSubtotalFromDetails.add(detailDTO.getSubtotal());
            }
        }

        // 3. Validar que la suma de los detalles coincida con el subtotal de la cabecera
        if (calculatedSubtotalFromDetails.compareTo(dto.getSubtotalAmount()) != 0) {
            throw new BusinessRuleException(
                    "Inconsistencia en el subtotal de la nota de crédito: " +
                            "El subtotal enviado (" + dto.getSubtotalAmount() + ") no coincide con la suma de los subtotales de los detalles (" + calculatedSubtotalFromDetails + ")."
            );
        }

        // 4. Validar que Subtotal + IVA == Total (esta ya la tenías)
        if (dto.getSubtotalAmount().add(dto.getVatAmount()).compareTo(dto.getTotalAmount()) != 0) {
            throw new BusinessRuleException("Inconsistencia en los totales: Subtotal + IVA no es igual al Total.");
        }

        // --- FIN DE LA NUEVA VALIDACIÓN ---

        // --- VALIDACIÓN DE DETALLES CONTRA LA VENTA ORIGINAL (CAMBIO CLAVE) ---
        // 1. Crear un conjunto de identificadores de los ítems de la VENTA ORIGINAL para una búsqueda eficiente.
        Set<Object> originalSaleItemKeys = sale.getSaleDetails().stream()
                .map(saleDetail -> saleDetail.getProduct() != null
                        ? (Object)saleDetail.getProduct().getIdProduct()
                        : saleDetail.getServiceName())
                .collect(Collectors.toSet());

        // 2. Iterar sobre los DTOs entrantes para validarlos ANTES de construir nada.
        if (dto.getDetails() != null) {
            for (CreditNoteDetailCreateDTO detailDTO : dto.getDetails()) {
                Object incomingKey = detailDTO.getProductId() != null
                        ? (Object)detailDTO.getProductId()
                        : detailDTO.getServiceName();

                // Si la clave del detalle entrante no estaba en el conjunto de claves originales, es un error.
                if (!originalSaleItemKeys.contains(incomingKey)) {
                    throw new BusinessRuleException(
                            "El detalle para el ítem '" + incomingKey + "' no puede ser añadido porque no existía en la venta original."
                    );
                }
            }
        }
        // --- FIN DE LA NUEVA VALIDACIÓN ---

        // 3. Validar duplicados en los detalles del DTO
        validateDetailsForDuplicates(dto.getDetails());

        // 4. Construir la entidad principal manualmente
        CreditNote newCreditNote = new CreditNote();
        Company companyRef = companyRepository.getReferenceById(companyId);
        newCreditNote.setCompany(companyRef);
        newCreditNote.setDocumentNumber(dto.getDocumentNumber());
        newCreditNote.setDescription(dto.getDescription());
        newCreditNote.setIssueDate(dto.getIssueDate());
        newCreditNote.setSale(sale);
        newCreditNote.setCreditNoteStatus("PENDIENTE"); // Estado inicial por defecto

        // --- ¡GUARDAR LOS NUEVOS CAMPOS FINANCIEROS! ---
        newCreditNote.setSubtotalAmount(dto.getSubtotalAmount());
        newCreditNote.setVatAmount(dto.getVatAmount());
        newCreditNote.setTotalAmount(dto.getTotalAmount());

        // 5. Construir y asociar los detalles
        for (CreditNoteDetailCreateDTO detailDTO : dto.getDetails()) {
            CreditNoteDetail newDetail = mapToCreditNoteDetail(detailDTO, newCreditNote);
            newCreditNote.addDetail(newDetail);
        }

        // 6. Persistir el grafo de objetos completo
        CreditNote savedCreditNote = creditNoteRepository.save(newCreditNote);
        return modelMapper.map(savedCreditNote, CreditNoteResponseDTO.class);
    }

    /**
     * Actualiza una nota de crédito y sus detalles.
     * Regla de negocio: solo se pueden modificar o eliminar detalles existentes. No se pueden añadir nuevos.
     */
    // En CreditNoteService.java

    @Transactional
    public CreditNoteResponseDTO updateCreditNote(Integer id, CreditNoteUpdateDTO dto) {
        CreditNote creditNote = creditNoteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nota de crédito con ID " + id + " no encontrada."));

        // 1. Validar que la nota de crédito esté en estado PENDIENTE
        if (!"PENDIENTE".equals(creditNote.getCreditNoteStatus())) {
            throw new BusinessRuleException("Solo se pueden editar notas de crédito con estado PENDIENTE. Estado actual: " + creditNote.getCreditNoteStatus());
        }

        // 2. Actualizar campos simples de la cabecera
        if (dto.getDocumentNumber() != null) {
            if (!dto.getDocumentNumber().equals(creditNote.getDocumentNumber()) &&
                    creditNoteRepository.existsByCompany_IdAndDocumentNumber(creditNote.getCompany().getId(), dto.getDocumentNumber())) {
                throw new BusinessRuleException("El número de documento " + dto.getDocumentNumber() + " ya está en uso.");
            }
            creditNote.setDocumentNumber(dto.getDocumentNumber());
        }
        if (dto.getDescription() != null) {
            creditNote.setDescription(dto.getDescription());
        }
        if (dto.getIssueDate() != null) {
            creditNote.setIssueDate(dto.getIssueDate());
        }
        // 3. Sincronizar detalles Y recalcular/validar totales si se proporciona la lista de detalles
        if (dto.getDetails() != null) {
            if (dto.getDetails().isEmpty()) {
                throw new BusinessRuleException("La lista de detalles no puede estar vacía si se incluye en la solicitud de actualización.");
            }
            if (dto.getSubtotalAmount() == null || dto.getVatAmount() == null || dto.getTotalAmount() == null) {
                throw new BusinessRuleException("Si se modifican los detalles, se deben enviar los nuevos valores de subtotalAmount, vatAmount y totalAmount.");
            }

            // --- INICIO DE VALIDACIÓN FINANCIERA REFORZADA ---

            BigDecimal calculatedSubtotalFromDetails = BigDecimal.ZERO;
            for (CreditNoteDetailCreateDTO detailDTO : dto.getDetails()) {
                // 3.1. Validar la consistencia de cada línea de detalle
                BigDecimal lineSubtotal = detailDTO.getUnitPrice().multiply(BigDecimal.valueOf(detailDTO.getQuantity()));
                if (lineSubtotal.compareTo(detailDTO.getSubtotal()) != 0) {
                    throw new BusinessRuleException(
                            "Inconsistencia en el detalle actualizado para el item '" + (detailDTO.getServiceName() != null ? detailDTO.getServiceName() : "Producto ID " + detailDTO.getProductId()) + "': " +
                                    "El subtotal enviado (" + detailDTO.getSubtotal() + ") no coincide con el cálculo."
                    );
                }
                calculatedSubtotalFromDetails = calculatedSubtotalFromDetails.add(detailDTO.getSubtotal());
            }

            // 3.2. Validar que la suma de los detalles coincida con el subtotal de la cabecera
            if (calculatedSubtotalFromDetails.compareTo(dto.getSubtotalAmount()) != 0) {
                throw new BusinessRuleException(
                        "Inconsistencia en el subtotal de la nota de crédito: " +
                                "El subtotal enviado (" + dto.getSubtotalAmount() + ") no coincide con la suma de los subtotales de los detalles (" + calculatedSubtotalFromDetails + ")."
                );
            }

            // 3.3. Validar que Subtotal + IVA == Total
            if (dto.getSubtotalAmount().add(dto.getVatAmount()).compareTo(dto.getTotalAmount()) != 0) {
                throw new BusinessRuleException("Inconsistencia en los totales: Subtotal + IVA no es igual al Total.");
            }

            // --- FIN DE VALIDACIÓN FINANCIERA REFORZADA ---


            // 4. Sincronizar los detalles con la base de datos
            Map<Integer, CreditNoteDetail> existingDetailsMap = creditNote.getDetails().stream()
                    .collect(Collectors.toMap(CreditNoteDetail::getCreditNoteDetailId, Function.identity()));
            Set<Integer> incomingDetailIds = new HashSet<>();

            for (CreditNoteDetailCreateDTO detailDTO : dto.getDetails()) {
                if (detailDTO.getCreditNoteDetailId() == null) {
                    throw new BusinessRuleException("No se pueden añadir nuevos detalles a una nota de crédito existente. Solo se permite actualizar.");
                }
                CreditNoteDetail existingDetail = existingDetailsMap.get(detailDTO.getCreditNoteDetailId());
                if (existingDetail == null) {
                    throw new BusinessRuleException("El detalle con ID " + detailDTO.getCreditNoteDetailId() + " no pertenece a esta nota de crédito.");
                }
                incomingDetailIds.add(detailDTO.getCreditNoteDetailId());

                // Actualizar el detalle existente
                existingDetail.setQuantity(detailDTO.getQuantity());
                existingDetail.setUnitPrice(detailDTO.getUnitPrice());
                existingDetail.setSubtotal(detailDTO.getSubtotal());
            }
            // Eliminar los detalles que ya no vienen en la solicitud (orphanRemoval=true se encargará de la BD)
            creditNote.getDetails().removeIf(detail -> !incomingDetailIds.contains(detail.getCreditNoteDetailId()));

            // 5. Actualizar los totales en la entidad principal
            creditNote.setSubtotalAmount(dto.getSubtotalAmount());
            creditNote.setVatAmount(dto.getVatAmount());
            creditNote.setTotalAmount(dto.getTotalAmount());
        }

        // 6. Guardar la entidad actualizada y devolver la respuesta
        CreditNote updatedCreditNote = creditNoteRepository.save(creditNote);
        return modelMapper.map(updatedCreditNote, CreditNoteResponseDTO.class);
    }

    /**
     * Elimina una nota de crédito por ID.
     */
    public void delete(Integer id) {
        // Buscamos la entidad para poder verificar su estado antes de eliminar
        CreditNote creditNote = creditNoteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nota de crédito con ID " + id + " no encontrada para eliminar."));

        //¡NUEVA REGLA DE NEGOCIO! Solo se pueden eliminar notas PENDIENTES.
        if (!"PENDIENTE".equals(creditNote.getCreditNoteStatus())) {
            throw new BusinessRuleException("Solo se pueden eliminar notas de crédito con estado PENDIENTE. Estado actual: " + creditNote.getCreditNoteStatus());
        }
        creditNoteRepository.delete(creditNote);
    }

    // --- Métodos de búsqueda ---

    /**
     * Busca notas de crédito por el ID de la venta asociada, DENTRO DE LA EMPRESA ACTUAL.
     */
    public List<CreditNoteResponseDTO> findBySaleId(Integer saleId) {
        // 1. Obtener el contexto de la empresa.
        Integer companyId = getCompanyIdFromContext();

        // 2. Antes de buscar las NC, es una buena práctica de seguridad verificar que la venta
        //    en sí misma pertenece a la empresa actual. El findById de SaleRepository ya está protegido.
        saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Venta con ID " + saleId + " no encontrada."));

        // 3. Llamar al método del repositorio que ahora es "tenant-aware".
        return creditNoteRepository.findByCompany_IdAndSale_SaleId(companyId, saleId).stream()
                .map(cn -> modelMapper.map(cn, CreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }


    /**
     * Busca notas de crédito por su estado, DENTRO DE LA EMPRESA ACTUAL.
     */
    public List<CreditNoteResponseDTO> findByStatus(String status) {
        // 1. Obtener el contexto de la empresa.
        Integer companyId = getCompanyIdFromContext();

        // 2. Llamar al método del repositorio que ahora es "tenant-aware".
        return creditNoteRepository.findByCompany_IdAndCreditNoteStatus(companyId, status).stream()
                .map(cn -> modelMapper.map(cn, CreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Busca notas de crédito por rango de fechas y opcionalmente por estado, DENTRO DE LA EMPRESA ACTUAL.
     */
    public List<CreditNoteResponseDTO> findByDateRangeAndStatus(LocalDate start, LocalDate end, String status) {
        // 1. Obtener el contexto de la empresa.
        Integer companyId = getCompanyIdFromContext();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        String finalStatus = (status != null && !status.isBlank()) ? status : null;

        // 2. Llamar al método del repositorio que ahora es "tenant-aware".
        return creditNoteRepository.findByCompanyIdAndDateRangeAndStatus(companyId, startDateTime, endDateTime, finalStatus).stream()
                .map(cn -> modelMapper.map(cn, CreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }
    /**
     * Aplica una nota de crédito, afectando el inventario y cambiando su estado.
     */
    @Transactional
    public CreditNoteResponseDTO applyCreditNote(Integer creditNoteId) {
        CreditNote creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new NotFoundException("Nota de crédito con ID " + creditNoteId + " no encontrada."));

        // 1. Validar el estado actual
        if (!"PENDIENTE".equals(creditNote.getCreditNoteStatus())) {
            throw new BusinessRuleException("Solo se pueden aplicar notas de crédito en estado PENDIENTE. Estado actual: " + creditNote.getCreditNoteStatus());
        }
        creditNote.setIssueDate(LocalDateTime.now()); // Oficializar la fecha al aplicar.
        // 2. Delegar la lógica de inventario al InventoryService
        // Esta es la operación de DEVOLUCIÓN, por lo que el stock AUMENTA.
        inventoryService.processCreditNoteApplication(creditNote);

        // 3. (Futuro) Aquí se llamaría a la lógica contable.
        salesAccountingService.createEntriesForCreditNoteApplication(creditNote);

        // --- NUEVA LÓGICA DE AJUSTE DE SALDO ---
        Customer customer = creditNote.getSale().getCustomer();
        BigDecimal newBalance = customer.getCurrentBalance().subtract(creditNote.getTotalAmount());
        customer.setCurrentBalance(newBalance);
        customerRepository.save(customer);
        // --- FIN ---

        // 4. Actualizar el estado de la nota de crédito
        creditNote.setCreditNoteStatus("APLICADA");
        CreditNote appliedCreditNote = creditNoteRepository.save(creditNote);

        return modelMapper.map(appliedCreditNote, CreditNoteResponseDTO.class);
    }

    /**
     * Anula una nota de crédito, revirtiendo la afectación al inventario.
     */
    @Transactional
    public CreditNoteResponseDTO cancelCreditNote(Integer creditNoteId) {
        CreditNote creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new NotFoundException("Nota de crédito con ID " + creditNoteId + " no encontrada."));

        // 1. Validar el estado actual
        if (!"APLICADA".equals(creditNote.getCreditNoteStatus())) {
            throw new BusinessRuleException("Solo se pueden anular notas de crédito en estado APLICADA. Estado actual: " + creditNote.getCreditNoteStatus());
        }

        // 2. Delegar la REVERSIÓN de inventario al InventoryService
        // Esto revertirá la devolución, por lo que el stock DISMINUYE.
        inventoryService.processCreditNoteCancellation(creditNote);

        // 3. Revertir la lógica contable.
        salesAccountingService.deleteEntriesForCreditNoteCancellation(creditNote);
        // --- INICIO DE LA LÓGICA DE REVERSIÓN DE SALDO ---
        Customer customer = creditNote.getSale().getCustomer();
        // Sumamos de nuevo el monto de la NC al saldo, porque la anulación de la NC
        // significa que la deuda original del cliente vuelve a estar vigente.
        BigDecimal newBalance = customer.getCurrentBalance().add(creditNote.getTotalAmount());
        customer.setCurrentBalance(newBalance);
        customerRepository.save(customer);
        // --- FIN DE LA LÓGICA DE REVERSIÓN DE SALDO ---

        // 4. Actualizar el estado de la nota de crédito
        creditNote.setCreditNoteStatus("ANULADA");
        CreditNote cancelledCreditNote = creditNoteRepository.save(creditNote);

        return modelMapper.map(cancelledCreditNote, CreditNoteResponseDTO.class);
    }
    // --- Métodos privados de ayuda ---

    private void validateDetailsForDuplicates(List<CreditNoteDetailCreateDTO> details) {
        if (details == null) return;
        Set<Object> seenKeys = new HashSet<>();
        for (CreditNoteDetailCreateDTO detailDTO : details) {
            Object key = detailDTO.getProductId() != null ? detailDTO.getProductId() : detailDTO.getServiceName();
            if (key == null || !seenKeys.add(key)) {
                throw new BusinessRuleException("La solicitud contiene detalles duplicados para el mismo producto o servicio.");
            }
        }
    }

    private CreditNoteDetail mapToCreditNoteDetail(CreditNoteDetailCreateDTO dto, CreditNote parent) {
        CreditNoteDetail detail = new CreditNoteDetail();
        detail.setCreditNoteDetailId(null); // Asegurar que sea una nueva entidad
        detail.setQuantity(dto.getQuantity());
        detail.setUnitPrice(dto.getUnitPrice());
        detail.setSubtotal(dto.getSubtotal());

        if (dto.getProductId() != null) {
            Product product = productService.findEntityById(dto.getProductId());
            detail.setProduct(product);
        } else if (dto.getServiceName() != null && !dto.getServiceName().isBlank()) {
            detail.setServiceName(dto.getServiceName());
        } else {
            throw new BusinessRuleException("Cada detalle debe tener un 'productId' o un 'serviceName'.");
        }
        return detail;
    }
}