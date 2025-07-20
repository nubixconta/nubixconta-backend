package com.nubixconta.modules.administration.repository;

import com.nubixconta.modules.administration.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Integer>, JpaSpecificationExecutor<Company> {
    List<Company> findByUser_UserName(String userName);
    boolean existsByCompanyName(String companyName);
    boolean existsByCompanyDui(String companyDui);
    boolean existsByCompanyNit(String companyNit);
    boolean existsByCompanyNrc(String companyNrc);
    List<Company> findByactiveStatus(boolean status);
}

