package com.nubixconta.modules.accountsreceivable.repository;

import com.nubixconta.modules.accountsreceivable.entity.AccountsReceivable;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CollectionDetailRepository extends JpaRepository<CollectionDetail, Integer> {

    @Query("SELECT a FROM CollectionDetail a WHERE a.CollectionDetailDate BETWEEN :start AND :end")
    List<CollectionDetail> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}