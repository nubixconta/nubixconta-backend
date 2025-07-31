package com.nubixconta.modules.administration.repository;

import com.nubixconta.modules.administration.entity.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Optional<AccessLog> findTopByUser_IdAndDateEndIsNullOrderByDateStartDesc(Integer userId);
}