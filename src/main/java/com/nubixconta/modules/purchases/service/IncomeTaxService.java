package com.nubixconta.modules.purchases.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.AccountsPayable.service.AccountsPayableService;
import com.nubixconta.modules.accounting.service.CierreContableService;
import com.nubixconta.modules.accounting.service.PurchasesAccountingService;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import com.nubixconta.modules.purchases.dto.incometax.IncomeTaxCreateDTO;
import com.nubixconta.modules.purchases.dto.incometax.IncomeTaxResponseDTO;
import com.nubixconta.modules.purchases.dto.incometax.IncomeTaxUpdateDTO;
import com.nubixconta.modules.purchases.entity.IncomeTax;
import com.nubixconta.modules.purchases.entity.Purchase;
import com.nubixconta.modules.purchases.repository.IncomeTaxRepository;
import com.nubixconta.modules.purchases.repository.PurchaseRepository;
import com.nubixconta.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncomeTaxService {

    // --- Repositorios y Servicios ---
    private final IncomeTaxRepository incomeTaxRepository;
    private final PurchaseRepository purchaseRepository;
    private final CompanyRepository companyRepository;
    private final ModelMapper modelMapper;
    private final ChangeHistoryService changeHistoryService;
    private final CierreContableService cierreContableService;

    // --- PUNTOS DE INTEGRACIÓN (SERVICIOS EXTERNOS) ---
    private final PurchasesAccountingService purchasesAccountingService;
    private final AccountsPayableService accountsPayableService;


    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

    // =========================================================================================
    // == MÉTODOS PÚBLICOS DEL CICLO DE VIDA
    // =========================================================================================

    @Transactional
    public IncomeTaxResponseDTO createIncomeTax(IncomeTaxCreateDTO dto) {
        Integer companyId = getCompanyIdFromContext();

        cierreContableService.verificarPeriodoAbierto(dto.getIssueDate().toLocalDate());

        // 1. Validaciones de Negocio
        if (incomeTaxRepository.existsByCompany_IdAndDocumentNumber(companyId, dto.getDocumentNumber())) {
            throw new BusinessRuleException("Ya existe una retención de ISR con el número de documento: " + dto.getDocumentNumber());
        }

        Purchase purchase = purchaseRepository.findById(dto.getPurchaseId())
                .orElseThrow(() -> new NotFoundException("La compra con ID " + dto.getPurchaseId() + " no fue encontrada."));

        if (!purchase.getCompany().getId().equals(companyId)) {
            throw new BusinessRuleException("Acción no permitida: La compra asociada no pertenece a la empresa actual.");
        }

        if (!"APLICADA".equals(purchase.getPurchaseStatus())) {
            throw new BusinessRuleException("Solo se pueden aplicar retenciones a compras en estado APLICADA.");
        }

        // Se comprueba si la compra tiene pagos activos (PENDIENTES o APLICADOS).
        // Si es así, se bloquea la creación de la retención.
        boolean hasActivePayments = accountsPayableService.validatePurchaseWithoutCollections(purchase.getIdPurchase());
        if (hasActivePayments) {
            throw new BusinessRuleException(
                    "No se puede crear la retención porque la compra asociada ya tiene pagos registrados. " +
                            "Por favor, anule primero los pagos en el módulo de Cuentas por Pagar."
            );
        }

        List<String> activeStatuses = List.of("PENDIENTE", "APLICADA");
        if (incomeTaxRepository.existsByCompany_IdAndPurchase_IdPurchaseAndIncomeTaxStatusIn(companyId, purchase.getIdPurchase(), activeStatuses)) {
            throw new BusinessRuleException("Ya existe una retención de ISR activa (PENDIENTE o APLICADA) para esta compra.");
        }

        if (dto.getAmountIncomeTax().compareTo(purchase.getTotalAmount()) > 0) {
            throw new BusinessRuleException("El monto de la retención (" + dto.getAmountIncomeTax() + ") no puede ser mayor al total de la compra (" + purchase.getTotalAmount() + ").");
        }

        // 2. Construcción de la Entidad
        Company companyRef = companyRepository.getReferenceById(companyId);
        IncomeTax newIncomeTax = new IncomeTax();
        newIncomeTax.setCompany(companyRef);
        newIncomeTax.setPurchase(purchase);
        newIncomeTax.setDocumentNumber(dto.getDocumentNumber());
        newIncomeTax.setDescription(dto.getDescription());
        newIncomeTax.setIssueDate(dto.getIssueDate());
        newIncomeTax.setAmountIncomeTax(dto.getAmountIncomeTax());
        newIncomeTax.setIncomeTaxStatus("PENDIENTE");

        IncomeTax savedIncomeTax = incomeTaxRepository.save(newIncomeTax);

        // 3. Bitácora
        changeHistoryService.logChange("Impuesto Sobre la Renta",
                String.format("Creó la retención N° %s por $%.2f, sobre la compra N° %s.",
                        savedIncomeTax.getDocumentNumber(), savedIncomeTax.getAmountIncomeTax(), savedIncomeTax.getPurchase().getDocumentNumber()));

        return modelMapper.map(savedIncomeTax, IncomeTaxResponseDTO.class);
    }

    @Transactional
    public IncomeTaxResponseDTO updateIncomeTax(Integer id, IncomeTaxUpdateDTO dto) {
        Integer companyId = getCompanyIdFromContext();
        IncomeTax incomeTax = incomeTaxRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Retención de ISR con ID " + id + " no encontrada."));
        cierreContableService.verificarPeriodoAbierto(incomeTax.getIssueDate().toLocalDate());

        if (!"PENDIENTE".equals(incomeTax.getIncomeTaxStatus())) {
            throw new BusinessRuleException("Solo se pueden editar retenciones en estado PENDIENTE.");
        }

        if (dto.getDocumentNumber() != null && !dto.getDocumentNumber().equals(incomeTax.getDocumentNumber()) &&
                incomeTaxRepository.existsByCompany_IdAndDocumentNumber(companyId, dto.getDocumentNumber())) {
            throw new BusinessRuleException("El número de documento " + dto.getDocumentNumber() + " ya está en uso.");
        }

        if (dto.getAmountIncomeTax() != null && dto.getAmountIncomeTax().compareTo(incomeTax.getPurchase().getTotalAmount()) > 0) {
            throw new BusinessRuleException("El monto de la retención no puede ser mayor al total de la compra.");
        }

        // Actualización de campos
        if (dto.getDocumentNumber() != null) incomeTax.setDocumentNumber(dto.getDocumentNumber());
        if (dto.getDescription() != null) incomeTax.setDescription(dto.getDescription());
        if (dto.getIssueDate() != null) incomeTax.setIssueDate(dto.getIssueDate());
        if (dto.getAmountIncomeTax() != null) incomeTax.setAmountIncomeTax(dto.getAmountIncomeTax());

        IncomeTax updatedIncomeTax = incomeTaxRepository.save(incomeTax);

        changeHistoryService.logChange("Impuesto Sobre la Renta",
                String.format("Actualizó la retención PENDIENTE N° %s.", updatedIncomeTax.getDocumentNumber()));

        return modelMapper.map(updatedIncomeTax, IncomeTaxResponseDTO.class);
    }

    @Transactional
    public void deleteIncomeTax(Integer id) {
        IncomeTax incomeTax = incomeTaxRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Retención de ISR con ID " + id + " no encontrada."));

        if (!"PENDIENTE".equals(incomeTax.getIncomeTaxStatus())) {
            throw new BusinessRuleException("Solo se pueden eliminar retenciones en estado PENDIENTE.");
        }

        changeHistoryService.logChange("Impuesto Sobre la Renta",
                String.format("Eliminó la retención PENDIENTE N° %s.", incomeTax.getDocumentNumber()));

        incomeTaxRepository.delete(incomeTax);
    }


    // =========================================================================================
    // == MÉTODOS DE ORQUESTACIÓN Y TRANSICIÓN DE ESTADO
    // =========================================================================================

    @Transactional
    public IncomeTaxResponseDTO applyIncomeTax(Integer id) {
        IncomeTax incomeTax = incomeTaxRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Retención de ISR con ID " + id + " no encontrada."));

        if (!"PENDIENTE".equals(incomeTax.getIncomeTaxStatus())) {
            throw new BusinessRuleException("Solo se pueden aplicar retenciones en estado PENDIENTE.");
        }

        // --- ORQUESTACIÓN DE INTEGRACIONES ---

        // 1. CONTABILIDAD: Generar el asiento contable de la retención.
        //    (Debe: Cuentas por Pagar Proveedor, Haber: ISR Retenido por Pagar)
        purchasesAccountingService.createEntryForIncomeTaxApplication(incomeTax);

        // 2. CUENTAS POR PAGAR: Se actualiza el monto a pagar, disminuyéndolo.
        accountsPayableService.updatePayableAmount(
                incomeTax.getPurchase().getIdPurchase(), // El ID de la compra original
                incomeTax.getAmountIncomeTax(),          // El monto del ISR a restar
                "APLICADA"                               // La operación que indica una resta
        );

        // 3. Actualizar estado y persistir
        incomeTax.setIncomeTaxStatus("APLICADA");
        IncomeTax appliedIncomeTax = incomeTaxRepository.save(incomeTax);

        changeHistoryService.logChange("Impuesto Sobre la Renta",
                String.format("Aplicó la retención N° %s. Estado cambió a APLICADA.", appliedIncomeTax.getDocumentNumber()));

        return modelMapper.map(appliedIncomeTax, IncomeTaxResponseDTO.class);
    }

    @Transactional
    public IncomeTaxResponseDTO cancelIncomeTax(Integer id) {
        IncomeTax incomeTax = incomeTaxRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Retención de ISR con ID " + id + " no encontrada."));

        if (!"APLICADA".equals(incomeTax.getIncomeTaxStatus())) {
            throw new BusinessRuleException("Solo se pueden anular retenciones en estado APLICADA.");
        }

        // Similar a la anulación de una compra, si la compra original ya tiene pagos,
        // no se debería poder anular una retención que afectó su saldo.
        boolean hasActivePayments = accountsPayableService.validatePurchaseWithoutCollections(incomeTax.getPurchase().getIdPurchase());
        if (hasActivePayments) {
            throw new BusinessRuleException(
                    "No se puede anular la retención porque la compra asociada ya tiene pagos registrados posteriores a la aplicación de este ISR."
            );
        }

        // --- ORQUESTACIÓN DE REVERSIONES ---

        // 1. CONTABILIDAD: Eliminar/Revertir el asiento contable de la retención.
        purchasesAccountingService.deleteEntryForIncomeTaxCancellation(incomeTax);

        // 2. CUENTAS POR PAGAR: Se actualiza el monto a pagar, restaurándolo (sumándolo de nuevo).
        accountsPayableService.updatePayableAmount(
                incomeTax.getPurchase().getIdPurchase(), // El ID de la compra original
                incomeTax.getAmountIncomeTax(),          // El monto del ISR a sumar
                "ANULADA"                                // La operación que indica una suma
        );

        // 3. Actualizar estado y persistir
        incomeTax.setIncomeTaxStatus("ANULADA");
        IncomeTax cancelledIncomeTax = incomeTaxRepository.save(incomeTax);

        changeHistoryService.logChange("Impuesto Sobre la Renta",
                String.format("Anuló la retención N° %s. Estado cambió a ANULADA.", cancelledIncomeTax.getDocumentNumber()));

        return modelMapper.map(cancelledIncomeTax, IncomeTaxResponseDTO.class);
    }

    // =========================================================================================
    // == MÉTODOS DE BÚSQUEDA
    // =========================================================================================

    @Transactional(readOnly = true)
    public List<IncomeTaxResponseDTO> findAll(String sortBy) {
        Integer companyId = getCompanyIdFromContext();
        List<IncomeTax> incomeTaxes;

        if ("status".equalsIgnoreCase(sortBy)) {
            incomeTaxes = incomeTaxRepository.findAllWithDetailsByCompanyIdOrderByStatus(companyId);
        } else {
            incomeTaxes = incomeTaxRepository.findAllWithDetailsByCompanyIdOrderByDate(companyId);
        }

        return incomeTaxes.stream()
                .map(it -> modelMapper.map(it, IncomeTaxResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncomeTaxResponseDTO findById(Integer id) {
        IncomeTax incomeTax = incomeTaxRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Retención de ISR con ID " + id + " no encontrada."));
        return modelMapper.map(incomeTax, IncomeTaxResponseDTO.class);
    }

    @Transactional(readOnly = true)
    public List<IncomeTaxResponseDTO> findByPurchaseId(Integer purchaseId) {
        Integer companyId = getCompanyIdFromContext();
        return incomeTaxRepository.findByCompany_IdAndPurchase_IdPurchase(companyId, purchaseId).stream()
                .map(it -> modelMapper.map(it, IncomeTaxResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IncomeTaxResponseDTO> findByStatus(String status) {
        Integer companyId = getCompanyIdFromContext();
        return incomeTaxRepository.findByCompany_IdAndIncomeTaxStatus(companyId, status.toUpperCase()).stream()
                .map(it -> modelMapper.map(it, IncomeTaxResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IncomeTaxResponseDTO> findByDateRangeAndStatus(LocalDate start, LocalDate end, String status) {
        Integer companyId = getCompanyIdFromContext();
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        String finalStatus = (status != null && !status.isBlank()) ? status.toUpperCase() : null;

        return incomeTaxRepository.findByCompanyIdAndDateRangeAndStatus(companyId, startDateTime, endDateTime, finalStatus).stream()
                .map(it -> modelMapper.map(it, IncomeTaxResponseDTO.class))
                .collect(Collectors.toList());
    }
}