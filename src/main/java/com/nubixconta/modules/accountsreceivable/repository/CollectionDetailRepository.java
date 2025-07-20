package com.nubixconta.modules.accountsreceivable.repository;

import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionDetailRepository extends JpaRepository<CollectionDetail, Integer> {
}