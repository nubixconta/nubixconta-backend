package com.nubixconta.modules.accountsreceivable.repository;

import com.nubixconta.modules.accountsreceivable.entity.CollectionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionEntryRepository extends JpaRepository<CollectionEntry, Integer> {
}
