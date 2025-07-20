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
    private final ModelMapper  modelMapper;

    @Autowired
    public CompanyService(CompanyRepository companyRepository,
                          UserRepository userRepository,
                          ChangeHistoryService changeHistoryService,
                          ModelMapper modelMapper) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.changeHistoryService = changeHistoryService;
        this.modelMapper = new ModelMapper();

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

        // Validación de consistencia entre activeStatus y companyStatus
        if (Boolean.FALSE.equals(dto.getActiveStatus()) && Boolean.TRUE.equals(dto.getCompanyStatus())) {
            throw new IllegalArgumentException("Una empresa inactiva no puede estar asignada (companyStatus = true).");
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


    public List<CompanyResponseDTO> getAllCompanies() {
        List<Company> companies = companyRepository.findAll();
        return companies.stream()
                .map(company -> modelMapper.map(company, CompanyResponseDTO.class))
                .toList();
    }
    public List<CompanyResponseDTO> getCompaniesByStatus(boolean status) {
        List<Company> companies = companyRepository.findByactiveStatus(status);
        return companies.stream()
                .map(company -> modelMapper.map(company, CompanyResponseDTO.class))
                .toList();
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

        StringBuilder cambios = new StringBuilder();

        // Validaciones de unicidad
        if (dto.getCompanyName() != null && !dto.getCompanyName().equals(company.getCompanyName())) {
            if (companyRepository.existsByCompanyName(dto.getCompanyName())) {
                throw new IllegalArgumentException("El nombre de empresa ya está registrado.");
            }
            cambios.append("Nombre cambiado de ").append(company.getCompanyName())
                    .append(" a ").append(dto.getCompanyName()).append(". ");
            company.setCompanyName(dto.getCompanyName());
        }

        if (dto.getCompanyDui() != null && !dto.getCompanyDui().equals(company.getCompanyDui())) {
            if (companyRepository.existsByCompanyDui(dto.getCompanyDui())) {
                throw new IllegalArgumentException("El DUI ya está registrado.");
            }
            cambios.append("El DUI cambio de ").append(company.getCompanyDui())
                    .append(" a ").append(dto.getCompanyDui()).append(". ");
            company.setCompanyDui(dto.getCompanyDui());
        }

        if (dto.getCompanyNit() != null && !dto.getCompanyNit().equals(company.getCompanyNit())) {
            if (companyRepository.existsByCompanyNit(dto.getCompanyNit())) {
                throw new IllegalArgumentException("El NIT ya está registrado.");
            }
            cambios.append("El NIT cambio de ").append(company.getCompanyNit())
                    .append(" a ").append(dto.getCompanyNit()).append(". ");
            company.setCompanyNit(dto.getCompanyNit());
        }

        if (dto.getCompanyNrc() != null && !dto.getCompanyNrc().equals(company.getCompanyNrc())) {
            if (companyRepository.existsByCompanyNrc(dto.getCompanyNrc())) {
                throw new IllegalArgumentException("El NRC ya está registrado.");
            }
            cambios.append("El NRC cambio de ").append(company.getCompanyNrc())
                    .append(" a ").append(dto.getCompanyNrc()).append(". ");
            company.setCompanyNrc(dto.getCompanyNrc());
        }
        // Validación de consistencia entre activeStatus y companyStatus
        if (Boolean.FALSE.equals(dto.getActiveStatus()) && Boolean.TRUE.equals(dto.getCompanyStatus())) {
            throw new IllegalArgumentException("Una empresa inactiva no puede estar asignada (companyStatus = true).");
        }

        if (dto.getCompanyStatus() != null && !dto.getCompanyStatus().equals(company.getCompanyStatus())) {
            String estadoAnterior = company.getCompanyStatus() ? "asignada" : "no asignada";
            String estadoNuevo = dto.getCompanyStatus() ? "asignada" : "no asignada";

            cambios.append("El estado de la empresa cambió de ")
                    .append(estadoAnterior)
                    .append(" a ")
                    .append(estadoNuevo)
                    .append(". ");

            company.setCompanyStatus(dto.getCompanyStatus());
        }

        if (dto.getActiveStatus() != null && !dto.getActiveStatus().equals(company.getActiveStatus())) {
            String estadoAnterior = company.getActiveStatus() ? "activa" : "inactiva";
            String estadoNuevo = dto.getActiveStatus() ? "activa" : "inactiva";

            cambios.append("El estado de actividad de la empresa cambió de ")
                    .append(estadoAnterior)
                    .append(" a ")
                    .append(estadoNuevo)
                    .append(". ");

            company.setActiveStatus(dto.getActiveStatus());
        }

        // Este campo no genera historial (como pediste)
        if (dto.getAccountId() != null) {
            company.setAccountId(dto.getAccountId());
        }

        if (dto.getUserId() != null && !dto.getUserId().equals(company.getUser().getId())) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
            cambios.append("Responsable cambiado de ID ")
                    .append(company.getUser().getId()).append(" a ").append(user.getId()).append(". ");
            company.setUser(user);
        }

        Company saved = companyRepository.save(company);

        // Si hubo cambios, registrar en bitácora
        if (!cambios.isEmpty()) {
            changeHistoryService.logChange(
                    "Administración",
                    cambios.toString(),
                    null
            );
        }

        return saved;
    }


    public List<Company> getCompaniesByUserName(String userName) {
        return companyRepository.findByUser_UserName(userName);
    }
}
