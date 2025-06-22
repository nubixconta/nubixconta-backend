package com.nubixconta.modules.administration.service;

import com.nubixconta.modules.administration.entity.ChangeHistory;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.ChangeHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChangeHistoryService {

    @Autowired
    private ChangeHistoryRepository changeHistoryRepository;

    /**
     * Registra una acción en la bitácora de cambios
     *
     * @param moduleName     El módulo donde ocurrió (Ej: "Ventas", "Clientes")
     * @param action         Descripción legible de la acción (Ej: "Se registró una venta con factura F001-0003")
     * @param userId         ID del usuario que realizó la acción
     * @param companyId      ID de la empresa relacionada
     */
    public void logChange(String moduleName, String action, Integer userId, Integer companyId) {
        System.out.println(">> Audit: userId=" + userId + ", companyId=" + companyId);
        ChangeHistory change = new ChangeHistory();
        change.setModuleName(moduleName);
        change.setActionPerformed(action);
        change.setDate(LocalDateTime.now());

        // Establecer el usuario
        User user = new User();
        user.setId(userId);
        change.setUser(user);

        // Establecer empresa solo si se proporciona
        if (companyId != null) {
            Company company = new Company();
            company.setId(companyId);
            change.setCompany(company);
        }

        changeHistoryRepository.save(change);
    }

}
