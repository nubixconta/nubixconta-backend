package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.AccountingSetting;
// --- ¡CAMBIO EN LA IMPORTACIÓN! ---
// Se importa la clase estática interna.
import com.nubixconta.modules.accounting.entity.AccountingSetting.AccountingSettingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para AccountingSetting, actualizado para usar la clave primaria compuesta (AccountingSettingId).
 */
@Repository
public interface AccountingSettingRepository extends JpaRepository<AccountingSetting, AccountingSettingId> {
    // Los métodos CRUD básicos como findById() ahora funcionarán con el objeto AccountingSettingId.
}