package com.nubixconta.modules.administration.repository;

import com.nubixconta.modules.administration.entity.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Optional<AccessLog> findTopByUser_IdAndDateEndIsNullOrderByDateStartDesc(Integer userId);
    // Nuevo método para buscar por un rango de fechas
    List<AccessLog> findByDateStartBetween(LocalDateTime start, LocalDateTime end);

    // Nuevo método para buscar por usuario
    List<AccessLog> findByUser_Id(Integer userId);

    // Nuevo método para buscar por usuario y por un rango de fechas
    List<AccessLog> findByUser_IdAndDateStartBetween(Integer userId, LocalDateTime start, LocalDateTime end);
}