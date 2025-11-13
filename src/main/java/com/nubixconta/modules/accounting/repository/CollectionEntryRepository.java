package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.CollectionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CollectionEntryRepository extends JpaRepository<CollectionEntry, Integer> {
    void deleteByCollectionDetailId(Integer collectionDetailId);
    //  método para encontrar todas las entradas por el ID del detalle
    List<CollectionEntry> findByCollectionDetail_Id(Integer collectionDetailId);
    List<CollectionEntry> findAll();

    @Query("SELECT ce FROM CollectionEntry ce JOIN FETCH ce.catalog c JOIN FETCH c.account a")
    List<CollectionEntry> findAllWithCatalogAndAccount();
}
