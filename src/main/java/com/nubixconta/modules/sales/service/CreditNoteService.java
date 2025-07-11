package com.nubixconta.modules.sales.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.service.ProductService;
import com.nubixconta.modules.sales.dto.creditnote.*;
import com.nubixconta.modules.sales.entity.CreditNote;
import com.nubixconta.modules.sales.entity.CreditNoteDetail;
import com.nubixconta.modules.sales.entity.Sale;
import com.nubixconta.modules.sales.repository.CreditNoteRepository;
import com.nubixconta.modules.sales.repository.SaleRepository;
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

    /**
     * Retorna todas las notas de crédito como DTOs.
     */
    public List<CreditNoteResponseDTO> findAll() {
        return creditNoteRepository.findAll().stream()
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
        // 1. Validar unicidad del número de documento
        if (creditNoteRepository.existsByDocumentNumber(dto.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe una nota de crédito con el número de documento: " + dto.getDocumentNumber());
        }
        if (creditNoteRepository.existsBySale_SaleId(dto.getSaleId())) {
            throw new BusinessRuleException("La venta con ID " + dto.getSaleId() + " ya tiene una nota de crédito asociada.");
        }
        // 2. Buscar la venta asociada
        Sale sale = saleRepository.findById(dto.getSaleId())
                .orElseThrow(() -> new NotFoundException("La venta con ID " + dto.getSaleId() + " no fue encontrada."));

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
        newCreditNote.setDocumentNumber(dto.getDocumentNumber());
        newCreditNote.setSale(sale);
        newCreditNote.setCreditNoteStatus("PENDIENTE"); // Estado inicial por defecto

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
    @Transactional
    public CreditNoteResponseDTO updateCreditNote(Integer id, CreditNoteUpdateDTO dto) {
        CreditNote creditNote = creditNoteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nota de crédito con ID " + id + " no encontrada."));

        if (dto.getDocumentNumber() != null && !dto.getDocumentNumber().equals(creditNote.getDocumentNumber()) &&
                creditNoteRepository.existsByDocumentNumber(dto.getDocumentNumber())) {
            throw new BusinessRuleException("El número de documento " + dto.getDocumentNumber() + " ya está en uso.");
        }

        creditNote.setDocumentNumber(dto.getDocumentNumber());
        creditNote.setCreditNoteStatus(dto.getCreditNoteStatus());

        Map<Integer, CreditNoteDetail> existingDetailsMap = creditNote.getDetails().stream()
                .collect(Collectors.toMap(CreditNoteDetail::getCreditNoteDetailId, Function.identity()));

        Set<Integer> incomingDetailIds = dto.getDetails().stream()
                .map(CreditNoteDetailCreateDTO::getCreditNoteDetailId)
                .collect(Collectors.toSet());

        for (CreditNoteDetailCreateDTO detailDTO : dto.getDetails()) {
            if (detailDTO.getCreditNoteDetailId() == null) {
                throw new BusinessRuleException("No se pueden añadir nuevos detalles. Solo modificar o eliminar existentes.");
            }

            CreditNoteDetail existingDetail = existingDetailsMap.get(detailDTO.getCreditNoteDetailId());

            if (existingDetail == null) {
                throw new BusinessRuleException("El detalle con ID " + detailDTO.getCreditNoteDetailId() + " no pertenece a esta nota de crédito.");
            }

            // Validación de seguridad para el subtotal
            BigDecimal calculatedSubtotal = detailDTO.getUnitPrice().multiply(new BigDecimal(detailDTO.getQuantity()));
            if (calculatedSubtotal.compareTo(detailDTO.getSubtotal()) != 0) {
                throw new BusinessRuleException(
                        "Inconsistencia en el cálculo para el detalle con ID " + detailDTO.getCreditNoteDetailId() +
                                ". Subtotal esperado: " + calculatedSubtotal + ", recibido: " + detailDTO.getSubtotal());
            }

            // Actualizar solo campos permitidos
            existingDetail.setQuantity(detailDTO.getQuantity());
            existingDetail.setUnitPrice(detailDTO.getUnitPrice());
            existingDetail.setSubtotal(detailDTO.getSubtotal());
        }

        // Eliminar detalles que ya no vienen en el DTO
        creditNote.getDetails().removeIf(detail -> !incomingDetailIds.contains(detail.getCreditNoteDetailId()));

        CreditNote updatedCreditNote = creditNoteRepository.save(creditNote);
        return modelMapper.map(updatedCreditNote, CreditNoteResponseDTO.class);
    }

    /**
     * Elimina una nota de crédito por ID.
     */
    public void delete(Integer id) {
        if (!creditNoteRepository.existsById(id)) {
            throw new NotFoundException("Nota de crédito con ID " + id + " no encontrada.");
        }
        creditNoteRepository.deleteById(id);
    }

    // --- Métodos de búsqueda ---

    public List<CreditNoteResponseDTO> findBySaleId(Integer saleId) {
        return creditNoteRepository.findBySale_SaleId(saleId).stream()
                .map(cn -> modelMapper.map(cn, CreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<CreditNoteResponseDTO> findByStatus(String status) {
        return creditNoteRepository.findByCreditNoteStatus(status).stream()
                .map(cn -> modelMapper.map(cn, CreditNoteResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<CreditNoteResponseDTO> findByDateRangeAndStatus(LocalDate start, LocalDate end, String status) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        String finalStatus = (status != null && !status.isBlank()) ? status : null;

        return creditNoteRepository.findByDateRangeAndStatus(startDateTime, endDateTime, finalStatus).stream()
                .map(cn -> modelMapper.map(cn, CreditNoteResponseDTO.class))
                .collect(Collectors.toList());
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