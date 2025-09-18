package com.nubixconta.modules.purchases.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import com.nubixconta.modules.purchases.dto.supplier.SupplierCreateDTO;
import com.nubixconta.modules.purchases.dto.supplier.SupplierResponseDTO;
import com.nubixconta.modules.purchases.dto.supplier.SupplierUpdateDTO;
import com.nubixconta.modules.purchases.entity.Supplier;
import com.nubixconta.modules.purchases.repository.SupplierRepository;
import com.nubixconta.modules.sales.entity.PersonType;
import com.nubixconta.security.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;
    private final ModelMapper modelMapper;
    private final ChangeHistoryService changeHistoryService;

    /**
     * Obtiene todos los proveedores activos de la empresa actual, ordenados por fecha de creación.
     */
    public List<SupplierResponseDTO> findAll() {
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));

        return supplierRepository.findByCompany_IdAndStatusTrueOrderByCreationDateDesc(companyId).stream()
                .map(s -> modelMapper.map(s, SupplierResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Busca un proveedor por su ID. Lanza NotFoundException si no se encuentra.
     */
    public SupplierResponseDTO findById(Integer id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proveedor con ID " + id + " no encontrado"));
        return modelMapper.map(supplier, SupplierResponseDTO.class);
    }

    /**
     * Obtiene la entidad Supplier por ID. Usado internamente o por otros servicios.
     */
    public Supplier findEntityById(Integer id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proveedor con ID " + id + " no encontrado"));
    }

    /**
     * Obtiene todos los proveedores inactivos de la empresa actual.
     */
    public List<SupplierResponseDTO> findInactive() {
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
        return supplierRepository.findByCompany_IdAndStatusFalseOrderByCreationDateDesc(companyId).stream()
                .map(s -> modelMapper.map(s, SupplierResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Busca proveedores activos usando filtros opcionales.
     */
    public List<SupplierResponseDTO> searchActive(String name, String lastName, String dui, String nit) {
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
        return supplierRepository.searchActive(companyId, name, lastName, dui, nit)
                .stream()
                .map(s -> modelMapper.map(s, SupplierResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuevo proveedor.
     */
    @Transactional
    public SupplierResponseDTO save(SupplierCreateDTO dto) {
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se puede crear un proveedor sin una empresa seleccionada."));

        Supplier supplier = modelMapper.map(dto, Supplier.class);

        // Limpia campos no aplicables según el tipo de persona antes de validar
        if (supplier.getPersonType() == PersonType.NATURAL) {
            supplier.setSupplierNit(null);
        } else if (supplier.getPersonType() == PersonType.JURIDICA) {
            supplier.setSupplierDui(null);
            supplier.setSupplierLastName(null);
        }

        validateSupplierBusinessRules(supplier, companyId);

        Company companyRef = companyRepository.getReferenceById(companyId);
        supplier.setCompany(companyRef);
        supplier.setStatus(true); // Los nuevos proveedores se crean como activos

        Supplier saved = supplierRepository.save(supplier);

        // Registro en Bitácora
        String logMessage = String.format("Creó el proveedor '%s'.", saved.getSupplierName());
        changeHistoryService.logChange("Compras - Proveedores", logMessage);

        return modelMapper.map(saved, SupplierResponseDTO.class);
    }

    /**
     * Actualiza un proveedor existente. No permite modificar DUI, NIT o NCR.
     */
    @Transactional
    public SupplierResponseDTO update(Integer id, SupplierUpdateDTO dto) {
        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proveedor con ID " + id + " no encontrado"));

        // --- INICIO DE LA LÓGICA DE ACTUALIZACIÓN PARCIAL CORREGIDA ---
        // Se aplica el nuevo valor solo si fue proporcionado en el DTO (no es null).
        if (dto.getSupplierName() != null) {
            existing.setSupplierName(dto.getSupplierName());
        }
        if (dto.getSupplierLastName() != null) {
            existing.setSupplierLastName(dto.getSupplierLastName());
        }
        if (dto.getAddress() != null) {
            existing.setAddress(dto.getAddress());
        }
        if (dto.getEmail() != null) {
            existing.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            existing.setPhone(dto.getPhone());
        }
        if (dto.getCreditDay() != null) {
            existing.setCreditDay(dto.getCreditDay());
        }
        if (dto.getCreditLimit() != null) {
            existing.setCreditLimit(dto.getCreditLimit());
        }
        if (dto.getExemptFromVat() != null) {
            existing.setExemptFromVat(dto.getExemptFromVat());
        }
        if (dto.getBusinessActivity() != null) {
            existing.setBusinessActivity(dto.getBusinessActivity());
        }
        if (dto.getAppliesPerception() != null) {
            existing.setAppliesPerception(dto.getAppliesPerception());
        }
        if (dto.getSupplierType() != null) {
            existing.setSupplierType(dto.getSupplierType());
        }
        // --- FIN DE LA LÓGICA CORREGIDA ---

        // Re-validar las reglas de negocio con los datos actualizados
        validateSupplierBusinessRules(existing, existing.getCompany().getId());

        Supplier updated = supplierRepository.save(existing);

        // Registro en Bitácora
        String logMessage = String.format("Actualizó los datos del proveedor '%s'.", updated.getSupplierName());
        changeHistoryService.logChange("Compras - Proveedores", logMessage);

        return modelMapper.map(updated, SupplierResponseDTO.class);
    }

    /**
     * Desactiva un proveedor.
     */
    @Transactional
    public void deactivate(Integer id) {
        Supplier supplier = findEntityById(id);

        if (!supplier.getStatus()) {
            throw new BusinessRuleException("El proveedor '" + supplier.getSupplierName() + "' ya se encuentra inactivo.");
        }

        supplier.setStatus(false);
        supplierRepository.save(supplier);

        // Registro en Bitácora
        String logMessage = String.format("Desactivó al proveedor '%s'.", supplier.getSupplierName());
        changeHistoryService.logChange("Compras - Proveedores", logMessage);
    }

    /**
     * Reactiva un proveedor.
     */
    @Transactional
    public void activate(Integer id) {
        Supplier supplier = findEntityById(id);

        if (supplier.getStatus()) {
            throw new BusinessRuleException("El proveedor '" + supplier.getSupplierName() + "' ya se encuentra activo.");
        }

        supplier.setStatus(true);
        supplierRepository.save(supplier);

        // Registro en Bitácora
        String logMessage = String.format("Reactivó al proveedor '%s'.", supplier.getSupplierName());
        changeHistoryService.logChange("Compras - Proveedores", logMessage);
    }

    /**
     * Validador central de reglas de negocio para la entidad Supplier.
     */
    private void validateSupplierBusinessRules(Supplier supplier, Integer companyId) {
        // --- Validación de unicidad a nivel de Tenant (Empresa) ---
        if (StringUtils.hasText(supplier.getSupplierDui())) {
            boolean exists = (supplier.getIdSupplier() == null)
                    ? supplierRepository.existsByCompany_IdAndSupplierDui(companyId, supplier.getSupplierDui())
                    : supplierRepository.existsByCompany_IdAndSupplierDuiAndIdSupplierNot(companyId, supplier.getSupplierDui(), supplier.getIdSupplier());
            if (exists) throw new BusinessRuleException("El DUI '" + supplier.getSupplierDui() + "' ya está registrado para un proveedor en esta empresa.");
        }
        if (StringUtils.hasText(supplier.getSupplierNit())) {
            boolean exists = (supplier.getIdSupplier() == null)
                    ? supplierRepository.existsByCompany_IdAndSupplierNit(companyId, supplier.getSupplierNit())
                    : supplierRepository.existsByCompany_IdAndSupplierNitAndIdSupplierNot(companyId, supplier.getSupplierNit(), supplier.getIdSupplier());
            if (exists) throw new BusinessRuleException("El NIT '" + supplier.getSupplierNit() + "' ya está registrado para un proveedor en esta empresa.");
        }
        if (StringUtils.hasText(supplier.getNrc())) {
            boolean exists = (supplier.getIdSupplier() == null)
                    ? supplierRepository.existsByCompany_IdAndNrc(companyId, supplier.getNrc())
                    : supplierRepository.existsByCompany_IdAndNrcAndIdSupplierNot(companyId, supplier.getNrc(), supplier.getIdSupplier());
            if (exists) throw new BusinessRuleException("El NCR '" + supplier.getNrc() + "' ya está registrado para un proveedor en esta empresa.");
        }

        // --- Validación de unicidad a nivel Global (Todo el sistema) ---
        if (StringUtils.hasText(supplier.getSupplierDui())) {
            boolean existsGlobally = (supplier.getIdSupplier() == null)
                    ? supplierRepository.existsByDuiGlobal(supplier.getSupplierDui())
                    : supplierRepository.existsByDuiGlobalAndIdSupplierNot(supplier.getSupplierDui(), supplier.getIdSupplier());
            if (existsGlobally) throw new BusinessRuleException("El DUI '" + supplier.getSupplierDui() + "' ya está registrado en el sistema.");
        }
        if (StringUtils.hasText(supplier.getSupplierNit())) {
            boolean existsGlobally = (supplier.getIdSupplier() == null)
                    ? supplierRepository.existsByNitGlobal(supplier.getSupplierNit())
                    : supplierRepository.existsByNitGlobalAndIdSupplierNot(supplier.getSupplierNit(), supplier.getIdSupplier());
            if (existsGlobally) throw new BusinessRuleException("El NIT '" + supplier.getSupplierNit() + "' ya está registrado en el sistema.");
        }
        if (StringUtils.hasText(supplier.getNrc())) {
            boolean existsGlobally = (supplier.getIdSupplier() == null)
                    ? supplierRepository.existsByNrcGlobal(supplier.getNrc())
                    : supplierRepository.existsByNcrGlobalAndIdSupplierNot(supplier.getNrc(), supplier.getIdSupplier());
            if (existsGlobally) throw new BusinessRuleException("El NCR '" + supplier.getNrc() + "' ya está registrado en el sistema.");
        }
        if (StringUtils.hasText(supplier.getEmail())) {
            boolean existsGlobally = (supplier.getIdSupplier() == null)
                    ? supplierRepository.existsByEmailGlobal(supplier.getEmail())
                    : supplierRepository.existsByEmailGlobalAndIdSupplierNot(supplier.getEmail(), supplier.getIdSupplier());
            if (existsGlobally) throw new BusinessRuleException("El Email '" + supplier.getEmail() + "' ya está registrado en el sistema.");
        }
        if (StringUtils.hasText(supplier.getPhone())) {
            boolean existsGlobally = (supplier.getIdSupplier() == null)
                    ? supplierRepository.existsByPhoneGlobal(supplier.getPhone())
                    : supplierRepository.existsByPhoneGlobalAndIdSupplierNot(supplier.getPhone(), supplier.getIdSupplier());
            if (existsGlobally) throw new BusinessRuleException("El Teléfono '" + supplier.getPhone() + "' ya está registrado en el sistema.");
        }

        // --- Reglas de negocio por Tipo de Persona ---
        if (supplier.getPersonType() == PersonType.NATURAL) {
            if (!StringUtils.hasText(supplier.getSupplierDui())) {
                throw new BusinessRuleException("Para una persona NATURAL, el DUI es obligatorio.");
            }
            if (StringUtils.hasText(supplier.getSupplierNit())) {
                throw new BusinessRuleException("Para una persona NATURAL, el NIT debe estar vacío.");
            }
        } else if (supplier.getPersonType() == PersonType.JURIDICA) {
            if (!StringUtils.hasText(supplier.getSupplierNit())) {
                throw new BusinessRuleException("Para una persona JURIDICA, el NIT es obligatorio.");
            }
            if (StringUtils.hasText(supplier.getSupplierDui())) {
                throw new BusinessRuleException("Para una persona JURIDICA, el DUI debe estar vacío.");
            }
            if (StringUtils.hasText(supplier.getSupplierLastName())) {
                throw new BusinessRuleException("Para una persona JURIDICA, el apellido debe estar vacío.");
            }
        }
    }
}