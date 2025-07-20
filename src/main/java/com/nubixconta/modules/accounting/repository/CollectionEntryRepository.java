package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.CollectionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionEntryRepository extends JpaRepository<CollectionEntry, Integer> {
    void deleteByCollectionDetailId(Integer collectionDetailId);
}
