package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.dto.reports.AccountBalanceDTO;
import com.nubixconta.modules.accounting.dto.reports.JournalMovementDetailDTO;
import com.nubixconta.modules.accounting.entity.GeneralLedgerView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nubixconta.modules.accounting.dto.reports.LedgerMovementDetailDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.repository.query.Param;

@Repository
public interface GeneralLedgerViewRepository extends JpaRepository<GeneralLedgerView, String> {

    /**
     * Obtiene todos los movimientos de un período para una empresa, enriquecidos
     * con los detalles de la cuenta (código y nombre) desde Catalog y Account.
     * Utiliza un DTO de proyección para eficiencia.
     */
    @Query("SELECT new com.nubixconta.modules.accounting.dto.reports.JournalMovementDetailDTO(" +
            "  v.documentId, v.documentType, v.accountingDate, v.idCatalog, " +
            "  COALESCE(c.customCode, a.generatedCode), COALESCE(c.customName, a.accountName), " +
            "  v.debe, v.haber, v.description) " +
            "FROM GeneralLedgerView v " +
            "JOIN Catalog c ON v.idCatalog = c.id " +
            "JOIN c.account a " +
            "WHERE v.companyId = :companyId AND v.accountingDate BETWEEN :startDate AND :endDate " +
            "ORDER BY v.accountingDate ASC, v.documentId ASC")
    List<JournalMovementDetailDTO> findJournalWithAccountDetails(Integer companyId, LocalDateTime startDate, LocalDateTime endDate);


    // Caso 1: Filtrar por Rango de Fechas (todas las cuentas)
    @Query("SELECT new com.nubixconta.modules.accounting.dto.reports.LedgerMovementDetailDTO(" +
            "  v.uniqueId, v.documentId, v.documentType, v.accountingDate, v.idCatalog, " +
            "  COALESCE(c.customCode, a.generatedCode), COALESCE(c.customName, a.accountName), " +
            "  v.debe, v.haber, v.description, v.companyId) " +
            "FROM GeneralLedgerView v JOIN Catalog c ON v.idCatalog = c.id JOIN c.account a " +
            "WHERE v.companyId = :companyId AND v.accountingDate BETWEEN :startDate AND :endDate " +
            "ORDER BY v.idCatalog, v.accountingDate ASC")
    List<LedgerMovementDetailDTO> findLedgerByDateRange(Integer companyId, LocalDateTime startDate, LocalDateTime endDate);

    // Caso 2: Filtrar por Rango de Fechas Y Cuenta
    @Query("SELECT new com.nubixconta.modules.accounting.dto.reports.LedgerMovementDetailDTO(" +
            "  v.uniqueId, v.documentId, v.documentType, v.accountingDate, v.idCatalog, " +
            "  COALESCE(c.customCode, a.generatedCode), COALESCE(c.customName, a.accountName), " +
            "  v.debe, v.haber, v.description, v.companyId) " +
            "FROM GeneralLedgerView v JOIN Catalog c ON v.idCatalog = c.id JOIN c.account a " +
            "WHERE v.companyId = :companyId AND v.idCatalog = :catalogId AND v.accountingDate BETWEEN :startDate AND :endDate " +
            "ORDER BY v.idCatalog, v.accountingDate ASC")
    List<LedgerMovementDetailDTO> findLedgerByDateRangeAndCatalog(Integer companyId, LocalDateTime startDate, LocalDateTime endDate, Integer catalogId);

    // Caso 3: Filtrar solo por Cuenta (todas las fechas)
    @Query("SELECT new com.nubixconta.modules.accounting.dto.reports.LedgerMovementDetailDTO(" +
            "  v.uniqueId, v.documentId, v.documentType, v.accountingDate, v.idCatalog, " +
            "  COALESCE(c.customCode, a.generatedCode), COALESCE(c.customName, a.accountName), " +
            "  v.debe, v.haber, v.description, v.companyId) " +
            "FROM GeneralLedgerView v JOIN Catalog c ON v.idCatalog = c.id JOIN c.account a " +
            "WHERE v.companyId = :companyId AND v.idCatalog = :catalogId " +
            "ORDER BY v.idCatalog, v.accountingDate ASC")
    List<LedgerMovementDetailDTO> findLedgerByCatalog(Integer companyId, Integer catalogId);

    // --- NUEVO MÉTODO PARA SALDOS INICIALES ---
    /**
     * Calcula el saldo acumulado (Debe - Haber) de cada cuenta desde el inicio
     * de los tiempos hasta una fecha de corte (exclusiva).
     * Es la base para calcular los Saldos Iniciales.
     */
    @Query("SELECT new com.nubixconta.modules.accounting.dto.reports.AccountBalanceDTO(" +
            "  v.idCatalog, SUM(v.debe - v.haber)) " +
            "FROM GeneralLedgerView v " +
            "WHERE v.companyId = :companyId AND v.accountingDate < :endDate " +
            "GROUP BY v.idCatalog")
    List<AccountBalanceDTO> getAccumulatedBalancesBefore(Integer companyId, LocalDateTime endDate);

    // --- NUEVO MÉTODO PARA MOVIMIENTOS DEL PERÍODO ---
    /**
     * Calcula los movimientos totales (Debe y Haber) de cada cuenta DENTRO de un período.
     * Es la base para los Movimientos del Período.
     */
    @Query("SELECT new com.nubixconta.modules.accounting.dto.reports.AccountBalanceDTO(" +
            "  v.idCatalog, SUM(v.debe), SUM(v.haber)) " +
            "FROM GeneralLedgerView v " +
            "WHERE v.companyId = :companyId AND v.accountingDate BETWEEN :startDate AND :endDate " +
            "GROUP BY v.idCatalog")
    List<AccountBalanceDTO> getPeriodMovements(Integer companyId, LocalDateTime startDate, LocalDateTime endDate);
}