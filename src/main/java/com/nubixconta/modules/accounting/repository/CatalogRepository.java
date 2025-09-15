package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Catalog.
 * Proporciona los métodos CRUD estándar para gestionar las activaciones de cuentas por empresa.
 */
@Repository
public interface CatalogRepository extends JpaRepository<Catalog, Integer> {

    // Método de ayuda útil para verificar si una cuenta ya está activada para una empresa.
    Optional<Catalog> findByCompany_IdAndAccount_Id(Integer companyId, Integer accountId);
}