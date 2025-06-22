package com.nubixconta.modules.administration.repository;

import com.nubixconta.modules.administration.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanyRepository extends JpaRepository<Company, Integer>, JpaSpecificationExecutor<Company> {
}
