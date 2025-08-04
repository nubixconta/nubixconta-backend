package com.nubixconta.modules.administration.service;

import com.nubixconta.modules.administration.dto.changehistory.ChangeHistoryCreateDTO;
import com.nubixconta.modules.administration.dto.changehistory.ChangeHistoryResponseDTO;
import com.nubixconta.modules.administration.entity.ChangeHistory;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.ChangeHistoryRepository;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.repository.UserRepository;
import com.nubixconta.security.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChangeHistoryService {

    private final ChangeHistoryRepository changeHistoryRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Autowired
    public ChangeHistoryService(ChangeHistoryRepository changeHistoryRepository,
                                UserRepository userRepository,
                                CompanyRepository companyRepository
    ) {
        this.changeHistoryRepository = changeHistoryRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Registra una acción en la bitácora de cambios
     *
     * @param moduleName El módulo donde ocurrió (Ej: "Ventas", "Clientes")
     * @param action     Descripción legible de la acción
     * @param userId     ID del usuario que realizó la acción
     * @param companyId  ID de la empresa relacionada (nullable)
     */
    public void logChange(ChangeHistoryCreateDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Company company = null;
        if (dto.getCompanyId() != null) {
            company = companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));
        }

        ChangeHistory change = new ChangeHistory();
        change.setUser(user);
        change.setCompany(company);
        change.setDate(dto.getDate());
        change.setActionPerformed(dto.getActionPerformed());
        change.setModuleName(dto.getModuleName());

        changeHistoryRepository.save(change);
    }

    //Metodo para registrar un cambio en cualquier modulo
    public void logChange(String moduleName, String actionPerformed) {
        // 1. Obtener el ID del usuario autenticado del token.
        Integer authenticatedUserId = JwtUtil.extractCurrentUserId();

        // 2. Extraer el token completo para poder obtener el companyId.
        String bearerToken = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest()
                .getHeader("Authorization");

        // 3. Extraer el companyId del token (si existe) utilizando el método ya creado.
        //    El método devuelve un Optional para manejar la posibilidad de que el ID no esté.
        Optional<Integer> companyIdOptional = JwtUtil.extractCompanyId(bearerToken.replace("Bearer ", ""));

        ChangeHistory history = new ChangeHistory();
        history.setUser(userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado")));
        history.setModuleName(moduleName);
        history.setActionPerformed(actionPerformed);
        history.setDate(LocalDateTime.now());

        // 4. Si el companyId está presente en el token, se agrega al registro.
        companyIdOptional.ifPresent(companyId -> {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));
            history.setCompany(company);
        });

        changeHistoryRepository.save(history);
    }

    public List<ChangeHistoryResponseDTO> getAllHistoryResponses() {
        // 1. Crear un objeto Sort para ordenar por el campo 'date' de forma descendente.
        Sort sortByDateDesc = Sort.by("date").descending();

        // 2. Usar el método findAll(Sort) del JpaRepository para obtener los datos ya ordenados.
        List<ChangeHistory> historyList = changeHistoryRepository.findAll(sortByDateDesc);

        return historyList.stream().map(history -> {
            ChangeHistoryResponseDTO dto = new ChangeHistoryResponseDTO();

            // Nombre del usuario (first + last)
            User user = history.getUser();
            dto.setUserFullName(user.getFirstName() + " " + user.getLastName());

            // Nombre de la empresa (si existe)
            Company company = history.getCompany();
            dto.setCompanyName(company != null ? company.getCompanyName() : "Sin empresa");

            // Campos propios de ChangeHistory
            dto.setModuleName(history.getModuleName());
            dto.setDate(history.getDate());
            dto.setActionPerformed(history.getActionPerformed());

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Obtiene las entradas de la bitácora para un rango de fechas.
     */
    public List<ChangeHistoryResponseDTO> getChangesByDateRange(
            LocalDateTime start, LocalDateTime end) {

        List<ChangeHistory> historyList;

        if (start != null && end != null) {
            // Asume que tienes un método en el repositorio que hace esto.
            historyList = changeHistoryRepository.findByDateBetween(start, end);
        } else {
            // Si no hay fechas, obtiene todo.
            historyList = changeHistoryRepository.findAll(Sort.by("date").descending());
        }

        // Convierte la lista de entidades a una lista de DTOs
        return historyList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    /**
     * Busca las entradas de la bitácora para un usuario específico,
     * opcionalmente dentro de un rango de fechas.
     * Devuelve una lista de DTOs para incluir los nombres de usuario y empresa.
     */
    public List<ChangeHistoryResponseDTO> getChangesByUserFiltered(
            Integer userId, LocalDateTime start, LocalDateTime end) {

        List<ChangeHistory> historyList;

        if (start != null && end != null) {
            // Llama al método existente que filtra por usuario y rango de fechas
            historyList = changeHistoryRepository.findByUserIdAndDateBetween(userId, start, end);
        } else {
            // Llama al método existente que obtiene todos los cambios del usuario ordenados
            historyList = changeHistoryRepository.findByUserId(userId, Sort.by("date").descending());
        }

        // Convierte la lista de entidades a una lista de DTOs
        return historyList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    private ChangeHistoryResponseDTO convertToDto(ChangeHistory history) {
        ChangeHistoryResponseDTO dto = new ChangeHistoryResponseDTO();

        // Obtiene el nombre completo del usuario
        User user = history.getUser();
        dto.setUserFullName(user.getFirstName() + " " + user.getLastName());

        // Obtiene el nombre de la empresa si existe
        Company company = history.getCompany();
        dto.setCompanyName(company != null ? company.getCompanyName() : "Sin empresa");

        // Otros campos de la bitácora
        dto.setModuleName(history.getModuleName());
        dto.setDate(history.getDate());
        dto.setActionPerformed(history.getActionPerformed());

        return dto;
    }


}
