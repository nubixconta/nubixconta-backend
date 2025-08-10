package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
import com.nubixconta.modules.sales.dto.customer.*;
import com.nubixconta.modules.sales.entity.Customer;
import com.nubixconta.modules.sales.repository.CustomerRepository;
import com.nubixconta.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.sales.entity.PersonType;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.stream.Collectors;
import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.administration.entity.Company; // <-- NUEVO IMPORT
import com.nubixconta.modules.administration.repository.CompanyRepository; // <-- NUEVO IMPORT
import com.nubixconta.security.TenantContext; // <-- NUEVO IMPORT

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final CompanyRepository companyRepository; // <-- NUEVO: NECESARIO PARA ASIGNAR LA EMPRESA

    // Obtener todos los clientes activos de la empresa actual
    public List<CustomerResponseDTO> findAll() {
        // Obtenemos el ID de la empresa del contexto de la petición actual
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));

        return customerRepository.findByCompany_IdAndStatusTrueOrderByCreationDateDesc(companyId).stream().map(
                        c -> modelMapper.map(c, CustomerResponseDTO.class))
                .collect(Collectors.toList());
    }

    // Buscar cliente por ID o lanzar excepción personalizada
    public CustomerResponseDTO findById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente con ID " + id + " no encontrado"));
        return modelMapper.map(customer, CustomerResponseDTO.class);
    }

    // Guardar nuevo cliente, vinculando el usuario autenticado
    public CustomerResponseDTO save(CustomerCreateDTO dto) {
        // 1. Obtener el companyId del contexto seguro.
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se puede crear un cliente sin una empresa seleccionada."));

        Customer customer = modelMapper.map(dto, Customer.class);

        // --- INICIO DE CAMBIOS ---
        // Limpiamos los campos que no correspondan ANTES de validar
        if (customer.getPersonType() == PersonType.NATURAL) {
            customer.setCustomerNit(null);

        } else if (customer.getPersonType() == PersonType.JURIDICA) {
            customer.setCustomerDui(null);
            customer.setCustomerLastName(null);
        }

        // 2. Llamamos a nuestro validador, ahora pasándole el companyId
        validateCustomerBusinessRules(customer, companyId);

        // 3. Obtenemos una referencia a la empresa para asignarla.
        //    Usar getReferenceById es eficiente, no necesita una consulta completa a la BD.
        Company companyRef = companyRepository.getReferenceById(companyId);
        customer.setCompany(companyRef);

        customer.setStatus(true); // por defecto activo

        Customer saved = customerRepository.save(customer);
        return modelMapper.map(saved, CustomerResponseDTO.class);
    }

    // Actualizar cliente (tipo PATCH, usando CustomerUpdateDTO)
    @Transactional
    public CustomerResponseDTO update(Integer id, CustomerUpdateDTO dto) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente con ID " + id + " no encontrado"));

        modelMapper.map(dto, existing);

        // La validación ahora usa el companyId de la entidad existente.
        validateCustomerBusinessRules(existing, existing.getCompany().getId());

        Customer updated = customerRepository.save(existing);
        return modelMapper.map(updated, CustomerResponseDTO.class);
    }

    // Eliminar cliente (físico o lógico)
    public void delete(Integer id) {
        if (!customerRepository.existsById(id)) {
            throw new NotFoundException("Cliente con ID " + id + " no encontrado");
        }
        customerRepository.deleteById(id);
    }

    // Buscar clientes activos con filtros
    public List<CustomerResponseDTO> searchActive(String name, String lastName, String dui, String nit) {
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
        // Pasa el companyId a la consulta modificada.
        return customerRepository.searchActive(companyId, name, lastName, dui, nit)
                .stream()
                .map(c -> modelMapper.map(c, CustomerResponseDTO.class))
                .collect(Collectors.toList());
    }
    // Obtener clientes inactivos
    public List<CustomerResponseDTO> findInactive() {
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
        return customerRepository.findByCompany_IdAndStatusFalse(companyId)
                .stream()
                .map(c -> modelMapper.map(c, CustomerResponseDTO.class))
                .collect(Collectors.toList());
    }

    // Utilidad: convertir cadenas vacías a null
    private String emptyToNull(String value) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }
    // Devuelve la entidad Customer. Protegido por el filtro de Hibernate.
    public Customer findEntityById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente con ID " + id + " no encontrado"));
    }

    // El método ahora recibe el companyId para usarlo en las validaciones de unicidad.
    private void validateCustomerBusinessRules(Customer customer, Integer companyId) {
        if (StringUtils.hasText(customer.getCustomerDui())) {
            boolean exists = (customer.getClientId() == null)
                    ? customerRepository.existsByCompany_IdAndCustomerDui(companyId, customer.getCustomerDui())
                    : customerRepository.existsByCompany_IdAndCustomerDuiAndClientIdNot(companyId, customer.getCustomerDui(), customer.getClientId());
            if (exists) throw new BusinessRuleException("El DUI '" + customer.getCustomerDui() + "' ya está registrado para esta empresa.");
        }
        if (StringUtils.hasText(customer.getCustomerNit())) {
            boolean exists = (customer.getClientId() == null)
                    ? customerRepository.existsByCompany_IdAndCustomerNit(companyId, customer.getCustomerNit())
                    : customerRepository.existsByCompany_IdAndCustomerNitAndClientIdNot(companyId, customer.getCustomerNit(), customer.getClientId());
            if (exists) throw new BusinessRuleException("El NIT '" + customer.getCustomerNit() + "' ya está registrado para esta empresa.");
        }
        if (StringUtils.hasText(customer.getNcr())) {
            boolean exists = (customer.getClientId() == null)
                    ? customerRepository.existsByCompany_IdAndNcr(companyId, customer.getNcr())
                    : customerRepository.existsByCompany_IdAndNcrAndClientIdNot(companyId, customer.getNcr(), customer.getClientId());
            if (exists) throw new BusinessRuleException("El NCR '" + customer.getNcr() + "' ya está registrado para esta empresa.");
        }
        // --- INICIO: NUEVA VALIDACIÓN GLOBAL ---
        // Verificamos DUI globalmente
        if (StringUtils.hasText(customer.getCustomerDui())) {
            boolean existsGlobally = (customer.getClientId() == null)
                    ? customerRepository.existsByDuiGlobal(customer.getCustomerDui())
                    : customerRepository.existsByDuiGlobalAndClientIdNot(customer.getCustomerDui(), customer.getClientId());
            if (existsGlobally) throw new BusinessRuleException("El DUI '" + customer.getCustomerDui() + "' ya está registrado en el sistema para otro cliente.");
        }

        // Verificamos NIT globalmente
        if (StringUtils.hasText(customer.getCustomerNit())) {
            boolean existsGlobally = (customer.getClientId() == null)
                    ? customerRepository.existsByNitGlobal(customer.getCustomerNit())
                    : customerRepository.existsByNitGlobalAndClientIdNot(customer.getCustomerNit(), customer.getClientId());
            if (existsGlobally) throw new BusinessRuleException("El NIT '" + customer.getCustomerNit() + "' ya está registrado en el sistema para otro cliente.");
        }

        // Verificamos NCR globalmente
        if (StringUtils.hasText(customer.getNcr())) {
            boolean existsGlobally = (customer.getClientId() == null)
                    ? customerRepository.existsByNcrGlobal(customer.getNcr())
                    : customerRepository.existsByNcrGlobalAndClientIdNot(customer.getNcr(), customer.getClientId());
            if (existsGlobally) throw new BusinessRuleException("El NCR '" + customer.getNcr() + "' ya está registrado en el sistema para otro cliente.");
        }

        // Verificamos Email globalmente
        if (StringUtils.hasText(customer.getEmail())) {
            boolean existsGlobally = (customer.getClientId() == null)
                    ? customerRepository.existsByEmailGlobal(customer.getEmail())
                    : customerRepository.existsByEmailGlobalAndClientIdNot(customer.getEmail(), customer.getClientId());
            if (existsGlobally) throw new BusinessRuleException("El Email '" + customer.getEmail() + "' ya está registrado en el sistema para otro cliente.");
        }

        // Verificamos Teléfono globalmente
        if (StringUtils.hasText(customer.getPhone())) {
            boolean existsGlobally = (customer.getClientId() == null)
                    ? customerRepository.existsByPhoneGlobal(customer.getPhone())
                    : customerRepository.existsByPhoneGlobalAndClientIdNot(customer.getPhone(), customer.getClientId());
            if (existsGlobally) throw new BusinessRuleException("El Teléfono '" + customer.getPhone() + "' ya está registrado en el sistema para otro cliente.");
        }
        // --- FIN: NUEVA VALIDACIÓN GLOBAL ---

        // La lógica condicional por tipo de persona no necesita cambios.
        if (customer.getPersonType() == PersonType.NATURAL) {
            if (!StringUtils.hasText(customer.getCustomerDui())) {
                throw new BusinessRuleException("Para una persona NATURAL, el DUI es obligatorio.");
            }
            if (StringUtils.hasText(customer.getCustomerNit())) {
                throw new BusinessRuleException("Para una persona NATURAL, el NIT debe estar vacío.");
            }
        } else if (customer.getPersonType() == PersonType.JURIDICA) {
            if (!StringUtils.hasText(customer.getCustomerNit())) {
                throw new BusinessRuleException("Para una persona JURIDICA, el NIT es obligatorio.");
            }
            if (StringUtils.hasText(customer.getCustomerDui())) {
                throw new BusinessRuleException("Para una persona JURIDICA, el DUI debe estar vacío.");
            }
            if (StringUtils.hasText(customer.getCustomerLastName())) {
                throw new BusinessRuleException("Para una persona JURIDICA, el apellido debe estar vacío.");
            }
        }
    }
}