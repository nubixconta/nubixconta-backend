package com.nubixconta.modules.administration.service;
import com.nubixconta.modules.administration.entity.ChangeHistory;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.ChangeHistoryRepository;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    @Autowired
    private ChangeHistoryRepository changeHistoryRepository;

    @Autowired
    private ChangeHistoryService changeHistoryService;
    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }
//Este metodo crea una empresa
    public Company saveCompany(Company company) {
        Company saved = companyRepository.save(company);

        // Buscar entrada antigua sin company_id
        List<ChangeHistory> pendientes = changeHistoryRepository
                .findByUserIdAndCompanyIsNullOrderByDateDesc(saved.getUser().getId());

        if (!pendientes.isEmpty()) {
            ChangeHistory entrada = pendientes.get(0); // Solo actualizas la más reciente
            entrada.setCompany(saved);
            changeHistoryRepository.save(entrada);
        }

        // Registrar creación de empresa en la entidad ChangeHistory
        String action = "Se creó la empresa " + saved.getCompanyName();
        changeHistoryService.logChange("Administración", action, saved.getUser().getId(), saved.getId());

        return saved;
    }
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public List<Company> searchCompanies(String companyName, String userName, Boolean status) {
        return companyRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (companyName != null && !companyName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("companyName")), "%" + companyName.toLowerCase() + "%"));
            }

            if (userName != null && !userName.isBlank()) {
                Join<Object, Object> userJoin = root.join("user"); // solo si se necesita
                Predicate firstNameLike = cb.like(cb.lower(userJoin.get("firstName")), "%" + userName.toLowerCase() + "%");
                Predicate lastNameLike = cb.like(cb.lower(userJoin.get("lastName")), "%" + userName.toLowerCase() + "%");
                predicates.add(cb.or(firstNameLike, lastNameLike));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("companyStatus"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
    //Metodo para actualizar la empresa
    public Company updateCompany(Integer id, Company updatedCompany) {
        return companyRepository.findById(id).map(existing -> {
            existing.setCompanyName(updatedCompany.getCompanyName());
            existing.setCompanyDui(updatedCompany.getCompanyDui());
            existing.setCompanyNit(updatedCompany.getCompanyNit());
            existing.setCompanyNrc(updatedCompany.getCompanyNrc());
            existing.setAccountId(updatedCompany.getAccountId());
            existing.setCompanyStatus(updatedCompany.getCompanyStatus());
            existing.setCreationDate(updatedCompany.getCreationDate());
            existing.setUser(updatedCompany.getUser());

            return companyRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + id));
    }

    public Company patchCompany(Integer id, Map<String, Object> fields) {
        return companyRepository.findById(id).map(company -> {
            fields.forEach((key, value) -> {
                try {
                    Field field = Company.class.getDeclaredField(key);
                    field.setAccessible(true);
                    field.set(company, value);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException("Error al actualizar el campo: " + key, e);
                }
            });
            return companyRepository.save(company);
        }).orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + id));
    }
    public List<Company> getCompaniesByUserName(String userName) {
        return companyRepository.findByUser_UserName(userName);
    }
}
