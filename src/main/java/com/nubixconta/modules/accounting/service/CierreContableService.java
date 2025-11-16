package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.accounting.dto.reports.CierreMensualStatusDTO;
import com.nubixconta.modules.accounting.entity.CierreMensual;
import com.nubixconta.modules.accounting.repository.CierreMensualRepository;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CierreContableService {

    private final CierreMensualRepository cierreRepository;
    private final CompanyRepository companyRepository; // Necesario para obtener la referencia de la empresa

    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("No se puede determinar la empresa del contexto."));
    }

    /**
     * Verifica si una fecha dada cae dentro de un período ya cerrado.
     * Lanza una excepción si el período está cerrado.
     * Este es el método "guardián" que se llamará desde otros servicios.
     */
    public void verificarPeriodoAbierto(LocalDate fecha) {
        Integer companyId = getCompanyIdFromContext();

        cierreRepository.findLatestClosedDate(companyId).ifPresent(ultimaFechaCerrada -> {
            if (!fecha.isAfter(ultimaFechaCerrada)) {
                throw new BusinessRuleException(
                        "La operación no puede realizarse con fecha " + fecha +
                                ". El período contable hasta " + ultimaFechaCerrada + " ya se encuentra cerrado."
                );
            }
        });
    }

    /**
     * Marca un mes específico como "cerrado".
     */
    @Transactional
    public void cerrarMes(int anio, int mes) {
        Integer companyId = getCompanyIdFromContext();

        // Lógica de validación (opcional pero recomendada): verificar que el mes anterior esté cerrado.

        CierreMensual cierre = cierreRepository.findByCompanyIdAndAnioAndMes(companyId, anio, mes)
                .orElseGet(() -> {
                    Company company = companyRepository.getReferenceById(companyId);
                    return new CierreMensual(company, anio, mes);
                });

        cierre.setCerrado(true);
        cierreRepository.save(cierre);
    }

    /**
     * Reabre un mes que estaba previamente cerrado.
     */
    @Transactional
    public void reabrirMes(int anio, int mes) {
        Integer companyId = getCompanyIdFromContext();

        // Validar que no se pueda reabrir un mes si el siguiente ya está cerrado.
        LocalDate siguienteMes = LocalDate.of(anio, mes, 1).plusMonths(1);
        cierreRepository.findByCompanyIdAndAnioAndMes(companyId, siguienteMes.getYear(), siguienteMes.getMonthValue())
                .ifPresent(cierreSiguiente -> {
                    if (cierreSiguiente.isCerrado()) {
                        throw new BusinessRuleException(
                                "No se puede reabrir el mes " + mes + "/" + anio +
                                        " porque el período siguiente (" + siguienteMes.getMonthValue() + "/" + siguienteMes.getYear() + ") ya está cerrado."
                        );
                    }
                });

        CierreMensual cierre = cierreRepository.findByCompanyIdAndAnioAndMes(companyId, anio, mes)
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró un registro de cierre para el mes " + mes + "/" + anio + ". No se puede reabrir."
                ));

        if (!cierre.isCerrado()) {
            throw new BusinessRuleException("El período " + mes + "/" + anio + " ya se encuentra abierto.");
        }

        cierre.setCerrado(false);
        cierreRepository.save(cierre);
    }

    /**
     * Obtiene el estado (abierto/cerrado) de los 12 meses de un año específico.
     * @param anio El año a consultar.
     * @return Una lista de 12 DTOs, uno para cada mes.
     */
    @Transactional(readOnly = true)
    public List<CierreMensualStatusDTO> getEstadosDeCierre(int anio) {
        Integer companyId = getCompanyIdFromContext();

        // 1. Obtener de la BD solo los registros de cierre que existen para ese año.
        Map<Integer, CierreMensual> cierresGuardados = cierreRepository.findByCompanyIdAndAnio(companyId, anio)
                .stream()
                .collect(Collectors.toMap(CierreMensual::getMes, cierre -> cierre));

        List<CierreMensualStatusDTO> estados = new ArrayList<>();
        Locale localeSpanish = new Locale("es", "ES");

        // 2. Iterar a través de los 12 meses del año para construir la respuesta completa.
        for (int i = 1; i <= 12; i++) {
            CierreMensual cierre = cierresGuardados.get(i);

            String nombreMes = Month.of(i).getDisplayName(TextStyle.FULL, localeSpanish);
            nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1); // Poner en mayúscula la primera letra

            // Si existe un registro y está marcado como cerrado, el estado es 'true'.
            // En cualquier otro caso (no existe registro, o existe pero está abierto), el estado es 'false'.
            boolean estaCerrado = cierre != null && cierre.isCerrado();

            estados.add(new CierreMensualStatusDTO(i, nombreMes, estaCerrado));
        }

        return estados;
    }
}