package com.nubixconta.modules.administration.repository;

import com.nubixconta.modules.administration.entity.ChangeHistory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Integer> {

    /**
     * Obtiene los cambios de un usuario que no están asociados a ninguna compañía,
     * ordenados por fecha descendente.
     */
    List<ChangeHistory> findByUserIdAndCompanyIsNullOrderByDateDesc(Integer userId);

    /**
     * Obtiene todos los cambios de un usuario sin orden garantizado.
     */
    List<ChangeHistory> findByUserId(Integer userId);

    /**
     * Obtiene todos los cambios de un usuario aplicando un Sort dinámico.
     * Ejemplo de uso:
     *    repository.findByUserId(userId, Sort.by("date").descending());
     */
    List<ChangeHistory> findByUserId(Integer userId, Sort sort);

    /**
     * Obtiene todos los cambios de un usuario ordenados por fecha descendente.
     * Útil si siempre quieres la misma ordenación sin pasar Sort.
     */
    List<ChangeHistory> findByUserIdOrderByDateDesc(Integer userId);

    /**
     * Busca todas las entradas entre dos fechas.
     */
    List<ChangeHistory> findByDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Busca todas las entradas de un usuario en un rango de fecha.
     */
    List<ChangeHistory> findByUserIdAndDateBetween(Integer userId,
                                                    LocalDateTime start,
                                                    LocalDateTime end);
}
