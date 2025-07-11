package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
import com.nubixconta.modules.sales.dto.customer.*;
import com.nubixconta.modules.sales.entity.Customer;
import com.nubixconta.modules.sales.repository.CustomerRepository;
import com.nubixconta.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import com.nubixconta.common.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;


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
    public CustomerResponseDTO update(Integer id, CustomerUpdateDTO dto) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente con ID " + id + " no encontrado"));

        modelMapper.map(dto, existing);
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

}