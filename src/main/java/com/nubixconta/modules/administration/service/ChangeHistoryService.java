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
import io.micrometer.common.lang.Nullable;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
    public void logChange(String moduleName, String actionPerformed, @Nullable Integer companyId) {
        Integer authenticatedUserId = JwtUtil.extractCurrentUserId();

        ChangeHistory history = new ChangeHistory();
        history.setUser(userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado")));
        history.setModuleName(moduleName);
        history.setActionPerformed(actionPerformed);
        history.setDate(LocalDateTime.now());

        if (companyId != null) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));
            history.setCompany(company);
        }

        changeHistoryRepository.save(history);
    }

    /**
     * Recupera los cambios “globales” de un usuario:
     * sólo aquellos donde company IS NULL, ordenados por fecha DESC.
     *
     * @param userId ID del usuario
     * @return lista de ChangeHistory
     */
    public List<ChangeHistory> getGlobalChangesByUser(Integer userId) {
        return changeHistoryRepository
                .findByUserIdAndCompanyIsNullOrderByDateDesc(userId);
    }

    /**
     * Recupera todos los cambios de un usuario (con o sin empresa).
     * Si necesitas ordenarlos, puedes pasar un Sort:
     * por ejemplo Sort.by("date").descending().
     *
     * @param userId ID del usuario
     * @return lista de ChangeHistory
     */
    public List<ChangeHistory> getAllChangesByUser(Integer userId) {
        return changeHistoryRepository.findByUserId(userId);
    }

    /**
     * Alternativa: recuperar todos los cambios con orden explícito
     */
    public List<ChangeHistory> getAllChangesByUserOrdered(Integer userId) {
        return changeHistoryRepository.findByUserId(
            userId, Sort.by("date").descending()
        );
    }

    /**
     * Entradas de un usuario SIN empresa asociada, ordenadas por fecha descendente
     */
    public List<ChangeHistory> getChangesWithoutCompany(Integer userId) {
        return changeHistoryRepository.findByUserIdAndCompanyIsNullOrderByDateDesc(userId);
    }

    /**
     * Entradas de un usuario dentro de un rango de fechas
     */
    public List<ChangeHistory> getChangesByUserAndDateBetween(Integer userId,
                                                                LocalDateTime start,
                                                                LocalDateTime end) {
        return changeHistoryRepository.findByUserIdAndDateBetween(userId, start, end);
    }

    public List<ChangeHistoryResponseDTO> getAllHistoryResponses() {
        List<ChangeHistory> historyList = changeHistoryRepository.findAll();

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
     * Todas las entradas en un rango de fechas, sin importar el usuario
     */
    public List<ChangeHistory> getByDateRange(LocalDateTime start,
                                                LocalDateTime end) {
        return changeHistoryRepository.findByDateBetween(start, end);
    }
}
