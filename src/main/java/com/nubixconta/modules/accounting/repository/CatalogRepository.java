package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogRepository extends JpaRepository<Catalog, Integer> {

    /**
     * Busca entradas del catálogo activas para una empresa específica,
     * filtrando por un término de búsqueda en el nombre o código de la cuenta contable.
     * Es ideal para los campos de autocompletar en la interfaz de usuario.
     *
     * @param companyId El ID de la empresa del contexto actual.
     * @param searchTerm El texto a buscar en el nombre o código de la cuenta.
     * @return Una lista de entradas del catálogo que coinciden con los criterios.
     */
    @Query("SELECT c FROM Catalog c JOIN c.account a WHERE c.company.id = :companyId " +
            "AND c.isActive = true " +
            "AND a.isPostable = true " +
            "AND (" +
            "   LOWER(COALESCE(c.customCode, a.generatedCode)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "   LOWER(COALESCE(c.customName, a.accountName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))" +
            ")")
    List<Catalog> searchActiveAndPostableByTerm(
            @Param("companyId") Integer companyId,
            @Param("searchTerm") String searchTerm
    );

    /**
     * Obtiene todas las entradas del catálogo activas y que aceptan movimientos para una empresa.
     *
     * @param companyId El ID de la empresa del contexto.
     * @return Una lista de todas las entradas del catálogo activas y que aceptan movimientos.
     */
    List<Catalog> findByCompany_IdAndIsActiveTrueAndAccount_IsPostableTrue(Integer companyId);

    // --- MÉTODO EXISTENTE (SE MANTIENE) ---
    /**
     * Método de ayuda útil para verificar si una cuenta ya está activada para una empresa.
     *
     * @param companyId El ID de la empresa.
     * @param accountId El ID de la cuenta maestra.
     * @return Un Optional que contiene la entrada del catálogo si existe.
     */
    Optional<Catalog> findByCompany_IdAndAccount_Id(Integer companyId, Integer accountId);

    /**
     * Encuentra todas las entradas del catálogo para una empresa específica,
     * trayendo eficientemente la entidad 'Account' asociada en la misma consulta
     * para evitar problemas de carga perezosa (Lazy Loading) y N+1.
     * @param companyId El ID de la empresa.
     * @return Una lista de entidades Catalog con sus Accounts precargadas.
     */
    @Query("SELECT c FROM Catalog c JOIN FETCH c.account WHERE c.company.id = :companyId")
    List<Catalog> findByCompany_IdWithAccount(@Param("companyId") Integer companyId);
}