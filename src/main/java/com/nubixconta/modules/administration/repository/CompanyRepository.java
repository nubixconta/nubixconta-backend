package com.nubixconta.modules.administration.repository;

import com.nubixconta.modules.administration.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Integer>, JpaSpecificationExecutor<Company> {
    List<Company> findByUser_UserName(String userName);
    List<Company> findByUser_Id(Integer userId);
    boolean existsByCompanyName(String companyName);
    boolean existsByCompanyDui(String companyDui);
    boolean existsByCompanyNit(String companyNit);
    boolean existsByCompanyNrc(String companyNrc);
    List<Company> findByactiveStatus(boolean status);

    // =========================================================================================
    // == INICIO DE CÓDIGO AÑADIDO: Verificación de Permisos
    // =========================================================================================
    /**
     * Comprueba si una empresa con un ID específico está asignada a un usuario con un ID específico.
     * Es una forma muy eficiente de validar la propiedad antes de generar un token de empresa.
     * @param companyId El ID de la empresa a comprobar.
     * @param userId El ID del usuario asignado.
     * @return true si la relación existe, false en caso contrario.
     */
    boolean existsByIdAndUser_Id(Integer companyId, Integer userId);
    // =========================================================================================
    // == FIN DE CÓDIGO AÑADIDO
    // =========================================================================================
}

