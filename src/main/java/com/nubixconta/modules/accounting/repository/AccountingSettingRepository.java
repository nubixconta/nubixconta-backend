package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.AccountingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingSettingRepository extends JpaRepository<AccountingSetting, String> {
}