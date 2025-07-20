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


@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    // Obtener todos los clientes activos
    public List<CustomerResponseDTO> findAll() {
        return customerRepository.findByStatusTrue().stream().map(
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
    public CustomerResponseDTO save(CustomerCreateDTO dto, HttpServletRequest request) {
        Customer customer = modelMapper.map(dto, Customer.class);

        // --- INICIO DE CAMBIOS ---
        // Limpiamos los campos que no correspondan ANTES de validar
        if (customer.getPersonType() == PersonType.NATURAL) {
            customer.setCustomerNit(null);

        } else if (customer.getPersonType() == PersonType.JURIDICA) {
            customer.setCustomerDui(null);
            customer.setCustomerLastName(null);
        }

        // Llamamos a nuestro nuevo validador centralizado
        validateCustomerBusinessRules(customer);
        // --- FIN DE CAMBIOS --

        // Obtener ID de usuario desde token JWT
        String token = request.getHeader("Authorization");
        Integer userId = JwtUtil.extractUserId(token);

        // Obtener entidad User o lanzar excepción personalizada
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario autenticado no encontrado"));

        customer.setUser(user);
        customer.setStatus(true); // por defecto activo

        Customer saved = customerRepository.save(customer);
        return modelMapper.map(saved, CustomerResponseDTO.class);
    }

    // Actualizar cliente (tipo PATCH, usando CustomerUpdateDTO)
    @Transactional
    public CustomerResponseDTO update(Integer id, CustomerUpdateDTO dto) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente con ID " + id + " no encontrado"));

        // Guardamos el tipo de persona original para comparar si cambió
        PersonType originalPersonType = existing.getPersonType();
        modelMapper.map(dto, existing);
        PersonType newPersonType = existing.getPersonType();

        // --- LÓGICA DE LIMPIEZA SI EL TIPO DE PERSONA CAMBIÓ ---
        if (newPersonType != originalPersonType) {
            // Si cambió a NATURAL, limpiamos los campos de JURIDICA
            if (newPersonType == PersonType.NATURAL) {
                existing.setCustomerNit(null);
            }
            // Si cambió a JURIDICA, limpiamos los campos de NATURAL
            else if (newPersonType == PersonType.JURIDICA) {
                existing.setCustomerDui(null);
                existing.setCustomerLastName(null);
            }
        }

        // Llamamos a nuestro validador centralizado con el estado final de la entidad
        validateCustomerBusinessRules(existing);

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
        // Pasa los parámetros directamente, sin el helper emptyToNull
        return customerRepository.searchActive(name, lastName, dui, nit)
                .stream()
                .map(c -> modelMapper.map(c, CustomerResponseDTO.class))
                .collect(Collectors.toList());
    }

    // Obtener clientes inactivos
    public List<CustomerResponseDTO> findInactive() {
        return customerRepository.findByStatusFalse()
                .stream()
                .map(c -> modelMapper.map(c, CustomerResponseDTO.class))
                .collect(Collectors.toList());
    }

    // Utilidad: convertir cadenas vacías a null
    private String emptyToNull(String value) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }
    // Devuelve la entidad Customer para uso interno en servicios
    public Customer findEntityById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente con ID " + id + " no encontrado"));
    }

    // --- ¡NUEVO MÉTODO DE VALIDACIÓN PRIVADO! ---
    private void validateCustomerBusinessRules(Customer customer) {
        // --- 1. VALIDACIÓN DE UNICIDAD (esta parte se mantiene igual y es correcta para NCR) ---
        if (StringUtils.hasText(customer.getCustomerDui())) {
            boolean exists = (customer.getClientId() == null)
                    ? customerRepository.existsByCustomerDui(customer.getCustomerDui())
                    : customerRepository.existsByCustomerDuiAndClientIdNot(customer.getCustomerDui(), customer.getClientId());
            if (exists) throw new BusinessRuleException("El DUI '" + customer.getCustomerDui() + "' ya está registrado.");
        }
        if (StringUtils.hasText(customer.getCustomerNit())) {
            boolean exists = (customer.getClientId() == null)
                    ? customerRepository.existsByCustomerNit(customer.getCustomerNit())
                    : customerRepository.existsByCustomerNitAndClientIdNot(customer.getCustomerNit(), customer.getClientId());
            if (exists) throw new BusinessRuleException("El NIT '" + customer.getCustomerNit() + "' ya está registrado.");
        }
        if (StringUtils.hasText(customer.getNcr())) {
            boolean exists = (customer.getClientId() == null)
                    ? customerRepository.existsByNcr(customer.getNcr())
                    : customerRepository.existsByNcrAndClientIdNot(customer.getNcr(), customer.getClientId());
            if (exists) throw new BusinessRuleException("El NCR '" + customer.getNcr() + "' ya está registrado.");
        }

        // --- 2. VALIDACIÓN CONDICIONAL POR TIPO DE PERSONA (SIMPLIFICADA) ---
        if (customer.getPersonType() == PersonType.NATURAL) {
            // Regla: Para NATURAL, DUI es obligatorio, NIT debe ser nulo.
            if (!StringUtils.hasText(customer.getCustomerDui())) {
                throw new BusinessRuleException("Para una persona NATURAL, el DUI es obligatorio.");
            }
            if (StringUtils.hasText(customer.getCustomerNit())) {
                throw new BusinessRuleException("Para una persona NATURAL, el NIT debe estar vacío.");
            }
            // YA NO SE VALIDA LA PRESENCIA/AUSENCIA DE NCR AQUÍ

        } else if (customer.getPersonType() == PersonType.JURIDICA) {
            // Regla: Para JURIDICA, NIT es obligatorio, DUI y Apellido deben ser nulos.
            if (!StringUtils.hasText(customer.getCustomerNit())) {
                throw new BusinessRuleException("Para una persona JURIDICA, el NIT es obligatorio.");
            }
            // YA NO SE VALIDA LA PRESENCIA/AUSENCIA DE NCR AQUÍ
            if (StringUtils.hasText(customer.getCustomerDui())) {
                throw new BusinessRuleException("Para una persona JURIDICA, el DUI debe estar vacío.");
            }
            if (StringUtils.hasText(customer.getCustomerLastName())) {
                throw new BusinessRuleException("Para una persona JURIDICA, el apellido debe estar vacío.");
            }
        }
    }

}