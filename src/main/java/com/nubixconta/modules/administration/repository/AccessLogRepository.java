package com.nubixconta.modules.administration.repository;

import com.nubixconta.modules.administration.entity.AccessLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Optional<AccessLog> findTopByUser_IdAndDateEndIsNullOrderByDateStartDesc(Integer userId);

    // Método modificado: acepta un Sort para ordenar por fecha
    List<AccessLog> findByDateStartBetween(LocalDateTime start, LocalDateTime end, Sort sort);

    // Método modificado: acepta un Sort para ordenar por fecha
    List<AccessLog> findByUser_Id(Integer userId, Sort sort);

    // Método modificado: acepta un Sort para ordenar por fecha
    List<AccessLog> findByUser_IdAndDateStartBetween(Integer userId, LocalDateTime start, LocalDateTime end, Sort sort);

}