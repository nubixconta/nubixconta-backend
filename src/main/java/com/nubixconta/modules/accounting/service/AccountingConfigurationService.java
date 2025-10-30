package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.entity.AccountingSetting;
// --- ¡CAMBIO EN LA IMPORTACIÓN! ---
import com.nubixconta.modules.accounting.entity.AccountingSetting.AccountingSettingId;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.repository.AccountingSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountingConfigurationService {

    private final AccountingSettingRepository settingRepository;

    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public Catalog findCatalogBySettingKey(String key, Integer companyId) {
        // --- ¡CAMBIO EN LA CREACIÓN DEL OBJETO! ---
        // Ahora se instancia usando la sintaxis de clase estática interna.
        AccountingSettingId settingId = new AccountingSetting.AccountingSettingId();
        settingId.setSettingKey(key);
        settingId.setCompanyId(companyId);

        AccountingSetting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> new BusinessRuleException("Configuración contable clave '" + key + "' no ha sido definida para esta empresa."));

        Catalog catalog = setting.getCatalog();
        Account account = catalog.getAccount();

        if (!catalog.isActive()) {
            throw new BusinessRuleException("La configuración para '" + key + "' apunta a una cuenta ('" + account.getAccountName() + "') que está actualmente desactivada para esta empresa.");
        }

        if (!account.isPostable()) {
            throw new BusinessRuleException("La cuenta '" + account.getAccountName() + "' configurada para '" + key + "' no es una cuenta de detalle (no es 'postable').");
        }

        return catalog;
    }
}