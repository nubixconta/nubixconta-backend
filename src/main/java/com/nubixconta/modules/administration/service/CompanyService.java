package com.nubixconta.modules.administration.service;
import com.nubixconta.modules.administration.dto.company.CompanyCreateDTO;
import com.nubixconta.modules.administration.dto.company.CompanyResponseDTO;
import com.nubixconta.modules.administration.dto.company.CompanyUpdateDTO;
import com.nubixconta.modules.administration.entity.ChangeHistory;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.ChangeHistoryRepository;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.repository.UserRepository;
import com.nubixconta.security.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CompanyService {


    private ChangeHistoryService changeHistoryService;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Autowired
    public CompanyService(CompanyRepository companyRepository,UserRepository userRepository,ChangeHistoryService changeHistoryService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.changeHistoryService = changeHistoryService;

    }
    //Este metodo crea una empresa
    public Company saveCompany(CompanyCreateDTO dto) {
    // Validaciones de unicidad
    if (companyRepository.existsByCompanyName(dto.getCompanyName())) {
        throw new IllegalArgumentException("El nombre de empresa ya está registrado.");
    }

    if (dto.getCompanyDui() != null && companyRepository.existsByCompanyDui(dto.getCompanyDui())) {
        throw new IllegalArgumentException("El DUI ya está registrado.");
    }

    if (dto.getCompanyNit() != null && companyRepository.existsByCompanyNit(dto.getCompanyNit())) {
        throw new IllegalArgumentException("El NIT ya está registrado.");
    }

    if (dto.getCompanyNrc() != null && companyRepository.existsByCompanyNrc(dto.getCompanyNrc())) {
        throw new IllegalArgumentException("El NRC ya está registrado.");
    }

    Company company = new Company();
    company.setCompanyName(dto.getCompanyName());
    company.setCompanyDui(dto.getCompanyDui());
    company.setCompanyNit(dto.getCompanyNit());
    company.setCompanyNrc(dto.getCompanyNrc());
    company.setCompanyStatus(dto.getCompanyStatus());
    company.setActiveStatus(dto.getActiveStatus());
    company.setCreationDate(dto.getCreationDate());
    company.setAccountId(dto.getAccountId());

    // Asignar relación con usuario
    User user = userRepository.findById(dto.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("Usuario indicado no encontrado"));
    company.setUser(user);


    // Guardar empresa
    Company saved = companyRepository.save(company);

    // Bitácora
        changeHistoryService.logChange(
                "Administración",
                "Se creó la empresa " + saved.getCompanyName(),
                null
        );

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

    public Company patchCompany(Integer id, CompanyUpdateDTO dto) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + id));

        // Validación de unicidad si se están cambiando los valores
        if (dto.getCompanyName() != null && !dto.getCompanyName().equals(company.getCompanyName())
                && companyRepository.existsByCompanyName(dto.getCompanyName())) {
            throw new IllegalArgumentException("El nombre de empresa ya está registrado.");
        }

        if (dto.getCompanyDui() != null && !dto.getCompanyDui().equals(company.getCompanyDui())
                && companyRepository.existsByCompanyDui(dto.getCompanyDui())) {
            throw new IllegalArgumentException("El DUI ya está registrado.");
        }

        if (dto.getCompanyNit() != null && !dto.getCompanyNit().equals(company.getCompanyNit())
                && companyRepository.existsByCompanyNit(dto.getCompanyNit())) {
            throw new IllegalArgumentException("El NIT ya está registrado.");
        }

        if (dto.getCompanyNrc() != null && !dto.getCompanyNrc().equals(company.getCompanyNrc())
                && companyRepository.existsByCompanyNrc(dto.getCompanyNrc())) {
            throw new IllegalArgumentException("El NRC ya está registrado.");
        }

        // Asignación solo si el valor viene en el DTO (no null)
        if (dto.getCompanyName() != null) company.setCompanyName(dto.getCompanyName());
        if (dto.getCompanyDui() != null) company.setCompanyDui(dto.getCompanyDui());
        if (dto.getCompanyNit() != null) company.setCompanyNit(dto.getCompanyNit());
        if (dto.getCompanyNrc() != null) company.setCompanyNrc(dto.getCompanyNrc());
        if (dto.getCompanyStatus() != null) company.setCompanyStatus(dto.getCompanyStatus());
        if (dto.getActiveStatus() != null) company.setActiveStatus(dto.getActiveStatus());
        if (dto.getAccountId() != null) company.setAccountId(dto.getAccountId());

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
            company.setUser(user);
        }

        return companyRepository.save(company);
    }

    public List<Company> getCompaniesByUserName(String userName) {
        return companyRepository.findByUser_UserName(userName);
    }
}
