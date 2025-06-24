package com.nubixconta.modules.administration.service;

import com.nubixconta.modules.administration.entity.ChangeHistory;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.ChangeHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChangeHistoryService {

    private final ChangeHistoryRepository changeHistoryRepository;

    @Autowired
    public ChangeHistoryService(ChangeHistoryRepository changeHistoryRepository) {
        this.changeHistoryRepository = changeHistoryRepository;
    }

    /**
     * Registra una acción en la bitácora de cambios
     *
     * @param moduleName El módulo donde ocurrió (Ej: "Ventas", "Clientes")
     * @param action     Descripción legible de la acción
     * @param userId     ID del usuario que realizó la acción
     * @param companyId  ID de la empresa relacionada (nullable)
     */
    public void logChange(String moduleName, String action, Integer userId, Integer companyId) {
        ChangeHistory change = new ChangeHistory();
        change.setModuleName(moduleName);
        change.setActionPerformed(action);
        change.setDate(LocalDateTime.now());

        // Asociar sólo el ID al usuario
        User user = new User();
        user.setId(userId);
        change.setUser(user);

        // Asociar sólo el ID a la empresa si se proporcionó
        if (companyId != null) {
            Company company = new Company();
            company.setId(companyId);
            change.setCompany(company);
        }

        changeHistoryRepository.save(change);
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

    /**
     * Todas las entradas en un rango de fechas, sin importar el usuario
     */
    public List<ChangeHistory> getByDateRange(LocalDateTime start,
                                                LocalDateTime end) {
        return changeHistoryRepository.findByDateBetween(start, end);
    }
}
