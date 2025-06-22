package com.nubixconta.modules.administration.repository;

import com.nubixconta.modules.administration.entity.ChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Integer> {
    List<ChangeHistory> findByUserIdAndCompanyIsNullOrderByDateDesc(Integer userId);
}

