package com.nubixconta.modules.accounting.service;

import com.nubixconta.modules.accounting.dto.reports.*;
import com.nubixconta.modules.accounting.dto.reports.JournalMovementDetailDTO;
import com.nubixconta.modules.accounting.dto.reports.EstadoResultadosLineaDTO;
import com.nubixconta.modules.accounting.dto.reports.EstadoResultadosResponseDTO;
import com.nubixconta.modules.accounting.repository.CatalogRepository;
import com.nubixconta.modules.accounting.repository.GeneralLedgerViewRepository;
import com.nubixconta.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FinancialReportService {

    private final GeneralLedgerViewRepository ledgerRepository;
    private final CatalogRepository catalogRepository;

    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("No se puede determinar la empresa del contexto."));
    }


    /**
     * Implementación del Libro Diario.
     * Ahora devuelve DTOs enriquecidos con código y nombre de cuenta.
     */
    @Transactional(readOnly = true)
    public List<LibroDiarioMovimientoDTO> getLibroDiario(LocalDate startDate, LocalDate endDate) {
        Integer companyId = getCompanyIdFromContext();

        // 1. Llamar al método del repositorio del Libro Diario.
        // El método devuelve una lista de 'JournalMovementDetailDTO'.
        // La variable 'movimientos' DEBE ser de este tipo.
        List<JournalMovementDetailDTO> movimientos = ledgerRepository.findJournalWithAccountDetails(
                companyId,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        // 2. Mapear la lista del DTO de proyección a la lista del DTO de respuesta final
        return movimientos.stream()
                .map(this::mapToLibroDiarioDTO) // Este método espera un JournalMovementDetailDTO
                .collect(Collectors.toList());
    }


    /**
     * Implementación del Libro Mayor con filtros opcionales.
     * @param startDate Fecha de inicio (opcional).
     * @param endDate Fecha de fin (opcional).
     * @param catalogId ID de la cuenta del catálogo a filtrar (opcional).
     * @return Una lista de cuentas, cada una con sus totales y detalle de movimientos.
     */
    @Transactional(readOnly = true)
    public List<LibroMayorCuentaDTO> getLibroMayor(LocalDate startDate, LocalDate endDate, Integer catalogId) {
        Integer companyId = getCompanyIdFromContext();
        List<LedgerMovementDetailDTO> movimientos;

        // Convertir LocalDate a LocalDateTime para la consulta
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        // --- LÓGICA DE DECISIÓN ---
        boolean hasDateRange = startDateTime != null && endDateTime != null;
        boolean hasCatalog = catalogId != null;

        if (hasDateRange && hasCatalog) {
            movimientos = ledgerRepository.findLedgerByDateRangeAndCatalog(companyId, startDateTime, endDateTime, catalogId);
        } else if (hasDateRange) {
            movimientos = ledgerRepository.findLedgerByDateRange(companyId, startDateTime, endDateTime);
        } else if (hasCatalog) {
            // Nota: Podríamos necesitar un manejo para "todas las fechas" si el volumen de datos es muy grande.
            // Por ahora, lo implementamos directamente.
            movimientos = ledgerRepository.findLedgerByCatalog(companyId, catalogId);
        } else {
            // Si el controlador no lo valida, devolvemos una lista vacía.
            return new ArrayList<>();
        }


        // 2. Agrupar los movimientos por ID de catálogo usando Java Streams
        // Usamos LinkedHashMap para mantener el orden de las cuentas
        return movimientos.stream()
                .collect(Collectors.groupingBy(LedgerMovementDetailDTO::getIdCatalog, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(movimientosDeUnaCuenta -> {
                    if (movimientosDeUnaCuenta.isEmpty()) {
                        return null; // Caso de seguridad
                    }
                    // El primer movimiento tiene la info de la cuenta
                    LedgerMovementDetailDTO primerMovimiento = movimientosDeUnaCuenta.get(0);

                    // 3. Calcular totales para esta cuenta
                    BigDecimal totalDebe = movimientosDeUnaCuenta.stream()
                            .map(LedgerMovementDetailDTO::getDebe)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalHaber = movimientosDeUnaCuenta.stream()
                            .map(LedgerMovementDetailDTO::getHaber)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 4. Crear la lista de movimientos detallados para el DTO
                    List<LibroMayorMovimientoDTO> detalleMovimientos = movimientosDeUnaCuenta.stream()
                            .map(m -> new LibroMayorMovimientoDTO(
                                    m.getAccountingDate(),
                                    m.getDocumentType(),
                                    m.getDocumentId(),
                                    m.getDescription(),
                                    m.getDebe(),
                                    m.getHaber()
                            ))
                            .collect(Collectors.toList());

                    // 5. Construir el DTO final para esta cuenta
                    LibroMayorCuentaDTO cuentaDTO = new LibroMayorCuentaDTO();
                    cuentaDTO.setIdCatalog(primerMovimiento.getIdCatalog());
                    cuentaDTO.setAccountCode(primerMovimiento.getAccountCode());
                    cuentaDTO.setAccountName(primerMovimiento.getAccountName());
                    cuentaDTO.setTotalDebe(totalDebe);
                    cuentaDTO.setTotalHaber(totalHaber);
                    cuentaDTO.setSaldoPeriodo(totalDebe.subtract(totalHaber));
                    cuentaDTO.setMovimientos(detalleMovimientos);

                    return cuentaDTO;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BalanzaComprobacionLineaDTO> getBalanzaDeComprobacion(LocalDate startDate, LocalDate endDate) {
        Integer companyId = getCompanyIdFromContext();

        // 1. Obtener los datos crudos de la BD en dos consultas eficientes
        Map<Integer, AccountBalanceDTO> saldosInicialesMap = ledgerRepository
                .getAccumulatedBalancesBefore(companyId, startDate.atStartOfDay())
                .stream().collect(Collectors.toMap(AccountBalanceDTO::getIdCatalog, Function.identity()));

        Map<Integer, AccountBalanceDTO> movimientosPeriodoMap = ledgerRepository
                .getPeriodMovements(companyId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX))
                .stream().collect(Collectors.toMap(AccountBalanceDTO::getIdCatalog, Function.identity()));

        // 2. Unir todas las cuentas que tuvieron algún movimiento
        Set<Integer> allAccountIds = Stream.concat(saldosInicialesMap.keySet().stream(), movimientosPeriodoMap.keySet().stream())
                .collect(Collectors.toSet());

        // Necesitamos la info del catálogo para saber el tipo de cuenta
        Map<Integer, com.nubixconta.modules.accounting.entity.Catalog> catalogMap = catalogRepository.findAllById(allAccountIds)
                .stream().collect(Collectors.toMap(com.nubixconta.modules.accounting.entity.Catalog::getId, Function.identity()));

        // 3. Procesar y combinar los resultados
        return allAccountIds.stream()
                .map(id -> {
                    AccountBalanceDTO saldoInicialData = saldosInicialesMap.getOrDefault(id, new AccountBalanceDTO(id, BigDecimal.ZERO));
                    AccountBalanceDTO movimientoData = movimientosPeriodoMap.getOrDefault(id, new AccountBalanceDTO(id, BigDecimal.ZERO, BigDecimal.ZERO));
                    com.nubixconta.modules.accounting.entity.Catalog catalog = catalogMap.get(id);

                    BigDecimal saldoInicial = saldoInicialData.getSaldo();
                    BigDecimal saldoFinal = saldoInicial.add(movimientoData.getTotalDebe()).subtract(movimientoData.getTotalHaber());

                    BalanzaComprobacionLineaDTO linea = new BalanzaComprobacionLineaDTO();
                    linea.setIdCatalog(id);
                    linea.setAccountCode(catalog.getEffectiveCode());
                    linea.setAccountName(catalog.getEffectiveName());
                    linea.setTotalDebePeriodo(movimientoData.getTotalDebe());
                    linea.setTotalHaberPeriodo(movimientoData.getTotalHaber());

                    // Lógica contable: Separar saldos en Deudor/Acreedor
                    String accountType = catalog.getAccount().getAccountType();
                    boolean esDeudora = accountType.startsWith("ACTIVO") || accountType.startsWith("GASTO") || accountType.startsWith("COSTO");

                    if (esDeudora) {
                        linea.setSaldoInicialDeudor(saldoInicial.signum() >= 0 ? saldoInicial : BigDecimal.ZERO);
                        linea.setSaldoInicialAcreedor(saldoInicial.signum() < 0 ? saldoInicial.abs() : BigDecimal.ZERO);
                        linea.setSaldoFinalDeudor(saldoFinal.signum() >= 0 ? saldoFinal : BigDecimal.ZERO);
                        linea.setSaldoFinalAcreedor(saldoFinal.signum() < 0 ? saldoFinal.abs() : BigDecimal.ZERO);
                    } else { // Cuentas Acreedoras (Pasivo, Patrimonio, Ingreso)
                        linea.setSaldoInicialDeudor(saldoInicial.signum() > 0 ? saldoInicial : BigDecimal.ZERO);
                        linea.setSaldoInicialAcreedor(saldoInicial.signum() <= 0 ? saldoInicial.abs() : BigDecimal.ZERO);
                        linea.setSaldoFinalDeudor(saldoFinal.signum() > 0 ? saldoFinal : BigDecimal.ZERO);
                        linea.setSaldoFinalAcreedor(saldoFinal.signum() <= 0 ? saldoFinal.abs() : BigDecimal.ZERO);
                    }
                    return linea;
                })
                .sorted(Comparator.comparing(BalanzaComprobacionLineaDTO::getAccountCode))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EstadoResultadosResponseDTO getEstadoDeResultados(LocalDate startDate, LocalDate endDate) {
        Integer companyId = getCompanyIdFromContext();

        // 1. REUTILIZAR: Obtener los movimientos del período. Es la misma llamada que en la Balanza.
        List<AccountBalanceDTO> movimientosPeriodo = ledgerRepository.getPeriodMovements(
                companyId,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        // 2. Obtener la información completa del catálogo para saber el tipo de cada cuenta.
        Set<Integer> accountIds = movimientosPeriodo.stream()
                .map(AccountBalanceDTO::getIdCatalog)
                .collect(Collectors.toSet());
        Map<Integer, com.nubixconta.modules.accounting.entity.Catalog> catalogMap = catalogRepository.findAllById(accountIds)
                .stream().collect(Collectors.toMap(com.nubixconta.modules.accounting.entity.Catalog::getId, Function.identity()));

        // 3. Inicializar listas y totales
        EstadoResultadosResponseDTO response = new EstadoResultadosResponseDTO();
        response.setIngresosOperacionales(new ArrayList<>());
        response.setCostoVenta(new ArrayList<>());
        response.setGastosVenta(new ArrayList<>());
        response.setGastosAdministracion(new ArrayList<>());
        response.setOtrosIngresos(new ArrayList<>());
        response.setOtrosGastos(new ArrayList<>());

        // 3. CLASIFICAR EN LAS NUEVAS SUBCATEGORÍAS
        for (AccountBalanceDTO movimiento : movimientosPeriodo) {
            com.nubixconta.modules.accounting.entity.Catalog catalog = catalogMap.get(movimiento.getIdCatalog());
            if (catalog == null || catalog.getAccount() == null) continue;

            String accountType = catalog.getAccount().getAccountType().toUpperCase().trim();

            EstadoResultadosLineaDTO linea = new EstadoResultadosLineaDTO();
            linea.setIdCatalog(catalog.getId());
            linea.setAccountCode(catalog.getEffectiveCode());
            linea.setAccountName(catalog.getEffectiveName());

            if (accountType.startsWith("INGRESO.OPERACIONAL")) {
                linea.setTotalPeriodo(movimiento.getTotalHaber().subtract(movimiento.getTotalDebe()));
                response.getIngresosOperacionales().add(linea);
            } else if (accountType.startsWith("INGRESO.NO_OPERACIONAL")) {
                linea.setTotalPeriodo(movimiento.getTotalHaber().subtract(movimiento.getTotalDebe()));
                response.getOtrosIngresos().add(linea);
            } else if (accountType.startsWith("COSTO")) { // COSTO.VENTAS, COSTO.COMPRAS
                linea.setTotalPeriodo(movimiento.getTotalDebe().subtract(movimiento.getTotalHaber()));
                response.getCostoVenta().add(linea);
            } else if (accountType.equals("GASTO.VENTA")) {
                linea.setTotalPeriodo(movimiento.getTotalDebe().subtract(movimiento.getTotalHaber()));
                response.getGastosVenta().add(linea);
            } else if (accountType.equals("GASTO.ADMINISTRACION")) {
                linea.setTotalPeriodo(movimiento.getTotalDebe().subtract(movimiento.getTotalHaber()));
                response.getGastosAdministracion().add(linea);
            } else if (accountType.startsWith("GASTO.FINANCIERO") || accountType.startsWith("GASTO.NO_OPERACIONAL")) {
                linea.setTotalPeriodo(movimiento.getTotalDebe().subtract(movimiento.getTotalHaber()));
                response.getOtrosGastos().add(linea);
            }
        }

        // 4. CALCULAR TOTALES Y SUBTOTALES EN CASCADA
        // Función helper para sumar listas de forma segura
        BiFunction<List<EstadoResultadosLineaDTO>, Function<EstadoResultadosLineaDTO, BigDecimal>, BigDecimal> sumList =
                (list, mapper) -> list.stream().map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIngresosOp = sumList.apply(response.getIngresosOperacionales(), EstadoResultadosLineaDTO::getTotalPeriodo);
        BigDecimal totalCostoVenta = sumList.apply(response.getCostoVenta(), EstadoResultadosLineaDTO::getTotalPeriodo);
        BigDecimal utilidadBruta = totalIngresosOp.subtract(totalCostoVenta);

        BigDecimal totalGastosVenta = sumList.apply(response.getGastosVenta(), EstadoResultadosLineaDTO::getTotalPeriodo);
        BigDecimal totalGastosAdmin = sumList.apply(response.getGastosAdministracion(), EstadoResultadosLineaDTO::getTotalPeriodo);
        BigDecimal totalGastosOp = totalGastosVenta.add(totalGastosAdmin);
        BigDecimal utilidadOperacional = utilidadBruta.subtract(totalGastosOp);

        BigDecimal totalOtrosIngresos = sumList.apply(response.getOtrosIngresos(), EstadoResultadosLineaDTO::getTotalPeriodo);
        BigDecimal totalOtrosGastos = sumList.apply(response.getOtrosGastos(), EstadoResultadosLineaDTO::getTotalPeriodo);
        BigDecimal utilidadAntesImpuestos = utilidadOperacional.add(totalOtrosIngresos).subtract(totalOtrosGastos);

        // 5. CÁLCULOS FINALES: RESERVA E IMPUESTOS
        // Nota: Estos porcentajes deberían venir de una configuración de la empresa en el futuro.
        BigDecimal porcentajeReserva = new BigDecimal("0.07");
        BigDecimal porcentajeImpuesto = new BigDecimal("0.30");

        BigDecimal reservaLegal = BigDecimal.ZERO;
        BigDecimal baseParaImpuesto = utilidadAntesImpuestos;

        // La reserva legal solo se calcula si hay utilidad
        if (utilidadAntesImpuestos.compareTo(BigDecimal.ZERO) > 0) {
            reservaLegal = utilidadAntesImpuestos.multiply(porcentajeReserva).setScale(2, RoundingMode.HALF_UP);
            baseParaImpuesto = utilidadAntesImpuestos.subtract(reservaLegal);
        }

        BigDecimal impuestoSobreLaRenta = BigDecimal.ZERO;
        // El impuesto solo se calcula si la base es positiva
        if (baseParaImpuesto.compareTo(BigDecimal.ZERO) > 0) {
            impuestoSobreLaRenta = baseParaImpuesto.multiply(porcentajeImpuesto).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal utilidadDelEjercicio = utilidadAntesImpuestos.subtract(reservaLegal).subtract(impuestoSobreLaRenta);

        // 6. POBLAR EL DTO DE RESPUESTA FINAL
        response.setTotalIngresosOperacionales(totalIngresosOp);
        response.setTotalCostoVenta(totalCostoVenta);
        response.setUtilidadBruta(utilidadBruta);
        response.setTotalGastosVenta(totalGastosVenta);
        response.setTotalGastosAdministracion(totalGastosAdmin);
        response.setTotalGastosOperacionales(totalGastosOp);
        response.setUtilidadOperacional(utilidadOperacional);
        response.setTotalOtrosIngresos(totalOtrosIngresos);
        response.setTotalOtrosGastos(totalOtrosGastos);
        response.setUtilidadAntesImpuestos(utilidadAntesImpuestos);
        response.setReservaLegal(reservaLegal);
        response.setImpuestoSobreLaRenta(impuestoSobreLaRenta);
        response.setUtilidadDelEjercicio(utilidadDelEjercicio);

        return response;
    }

    private LibroDiarioMovimientoDTO mapToLibroDiarioDTO(JournalMovementDetailDTO projection) {
        LibroDiarioMovimientoDTO dto = new LibroDiarioMovimientoDTO();
        dto.setDocumentId(projection.getDocumentId());
        dto.setDocumentType(projection.getDocumentType());
        dto.setAccountingDate(projection.getAccountingDate());
        dto.setIdCatalog(projection.getIdCatalog());
        dto.setAccountCode(projection.getAccountCode());
        dto.setAccountName(projection.getAccountName());
        dto.setDebe(projection.getDebe());
        dto.setHaber(projection.getHaber());
        dto.setDescription(projection.getDescription());
        return dto;
    }
}