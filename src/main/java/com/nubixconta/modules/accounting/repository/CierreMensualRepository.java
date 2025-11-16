package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.CierreMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CierreMensualRepository extends JpaRepository<CierreMensual, Long> {

    /**
     * Busca el último día del último mes que ha sido marcado como "cerrado" para una empresa.
     * @param companyId El ID de la empresa.
     * @return Un Optional que contiene la fecha del último cierre si existe.
     */
    @Query("SELECT MAX(cm.fechaCierre) FROM CierreMensual cm WHERE cm.company.id = :companyId AND cm.cerrado = true")
    Optional<LocalDate> findLatestClosedDate(Integer companyId);

    /**
     * Busca un registro de cierre específico por empresa, año y mes.
     * @param companyId El ID de la empresa.
     * @param anio El año.
     * @param mes El mes.
     * @return Un Optional que contiene el registro de cierre si existe.
     */
    Optional<CierreMensual> findByCompanyIdAndAnioAndMes(Integer companyId, int anio, int mes);

    /**
     * Busca todos los registros de cierre para una empresa y un año específicos.
     * @param companyId El ID de la empresa.
     * @param anio El año a consultar.
     * @return Una lista de los registros de cierre encontrados para ese año.
     */
    List<CierreMensual> findByCompanyIdAndAnio(Integer companyId, int anio);
}