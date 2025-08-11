package com.nubixconta.modules.accountsreceivable.repository;


import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CollectionDetailRepository extends JpaRepository<CollectionDetail, Integer> {

    // Nuevo método para encontrar todos por empresa
    List<CollectionDetail> findByCompanyId(Integer companyId);

    // Modificar el método existente para incluir el companyId
    List<CollectionDetail> findByCompanyIdAndCollectionDetailDateBetween(Integer companyId, LocalDateTime start, LocalDateTime end);

    // Repositorio
    @Query("SELECT a FROM CollectionDetail a WHERE a.accountReceivable.id = :receivableId AND a.company.id = :companyId")
    List<CollectionDetail> findByAccountReceivableIdAndCompanyId(@Param("receivableId") Integer receivableId, @Param("companyId") Integer companyId);

    @Query("SELECT a FROM CollectionDetail a WHERE a.collectionDetailDate BETWEEN :start AND :end")
    List<CollectionDetail> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}