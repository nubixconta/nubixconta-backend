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
        // Validación: No se pueden registrar DUI y NIT simultáneamente
        if (dto.getCompanyDui() != null && dto.getCompanyNit() != null) {
            throw new IllegalArgumentException("No se puede registrar una empresa con DUI y NIT a la vez. Debe ser uno o el otro.");
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
/*
        // Validación de consistencia entre activeStatus y companyStatus
        if (Boolean.FALSE.equals(dto.getActiveStatus()) && Boolean.TRUE.equals(dto.getCompanyStatus())) {
            throw new IllegalArgumentException("Una empresa inactiva no puede estar asignada (companyStatus = true).");
        }

*/
        Company company = new Company();
    company.setCompanyName(dto.getCompanyName());
    company.setCompanyDui(dto.getCompanyDui());
    company.setCompanyNit(dto.getCompanyNit());
    company.setCompanyNrc(dto.getCompanyNrc());
    company.setCreationDate(dto.getCreationDate());
    company.setTurnCompany(dto.getTurnCompany());
    company.setAddress(dto.getAddress());
    company.setImageUrl(dto.getImageUrl());

/*
        // Nueva validación: no se puede asignar empresa (companyStatus=true) sin usuario
        if (dto.getUserId() == null && Boolean.TRUE.equals(dto.getCompanyStatus())) {
            throw new IllegalArgumentException("No se puede asignar la empresa si no hay un usuario asociado (companyStatus = true).");
        }

 */
    // Asignar relación con usuario
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Usuario indicado no encontrado"));
            company.setUser(user);
        }

    // Guardar empresa
    Company saved = companyRepository.save(company);

    // Bitácora
        changeHistoryService.logChange(
                "Administración",
                "Se creó la empresa " + saved.getCompanyName()
        );

    return saved;
}


    public List<CompanyResponseDTO> getAllCompanies() {
        List<Company> companies = companyRepository.findAll();
        return companies.stream()
                .map(company -> modelMapper.map(company, CompanyResponseDTO.class))
                .toList();
    }
    //Metodo para listar todas las empresas activas
    public List<CompanyResponseDTO> getCompaniesByStatus(boolean status) {
        List<Company> companies = companyRepository.findByactiveStatus(status);
        return companies.stream()
                .map(company -> modelMapper.map(company, CompanyResponseDTO.class))
                .toList();
    }
    //Metodo para listar todas las empresas activas  y asignadas
    public List<CompanyResponseDTO> getCompaniesByActiveAndAssigned(boolean activeStatus, Boolean companyStatus) {
        List<Company> companies = companyRepository.findByActiveStatusAndCompanyStatus(activeStatus,companyStatus);
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

        //  LÓGICA PARA GIRO
        if (dto.getTurnCompany() != null && !dto.getTurnCompany().equals(company.getTurnCompany())) {
            cambios.append("El giro cambió de ").append(company.getTurnCompany())
                    .append(" a ").append(dto.getTurnCompany()).append(". ");
            company.setTurnCompany(dto.getTurnCompany());
        }

        // LÓGICA PARA DIRECCIÓN
        if (dto.getAddress() != null && !dto.getAddress().equals(company.getAddress())) {
            cambios.append("La dirección cambió de ").append(company.getAddress())
                    .append(" a ").append(dto.getAddress()).append(". ");
            company.setAddress(dto.getAddress());
        }

        // Validación de consistencia entre activeStatus y companyStatus
        if (Boolean.FALSE.equals(dto.getActiveStatus()) && Boolean.TRUE.equals(dto.getCompanyStatus())) {
            throw new IllegalArgumentException("Una empresa inactiva no puede estar asignada (companyStatus = true).");
        }

        // Validación: si se quiere asignar (companyStatus = true), debe tener usuario asignado
        if (Boolean.TRUE.equals(dto.getCompanyStatus())) {
            boolean usuarioAsignado =
                    dto.getUserId() != null || company.getUser() != null;

            if (!usuarioAsignado) {
                throw new IllegalArgumentException("Una empresa asignada debe tener un usuario responsable.");
            }
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



        if (dto.getImageUrl() != null && !dto.getImageUrl().equals(company.getImageUrl())) {
            cambios.append("La imagen de la empresa cambio ");
            company.setImageUrl(dto.getImageUrl());
        }



        if (dto.getUserId() != null &&
                (company.getUser() == null || !dto.getUserId().equals(company.getUser().getId()))) {

            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

            if (company.getUser() != null) {
                cambios.append("Responsable cambiado de ID ")
                        .append(company.getUser().getId())
                        .append(" a ")
                        .append(user.getId())
                        .append(". ");
            } else {
                cambios.append("Responsable asignado ")
                        .append(user.getFirstName()+user.getLastName())
                        .append(". ");
            }

            company.setUser(user);
        }

        Company saved = companyRepository.save(company);

        // Si hubo cambios, registrar en bitácora
        if (!cambios.isEmpty()) {
            changeHistoryService.logChange(
                    "Administración",
                    cambios.toString()
            );
        }

        return saved;
    }


    public List<Company> getCompaniesByUserName(String userName) {
        return companyRepository.findByUser_UserName(userName);
    }


    public List<Company> getCompaniesByUserId(Integer userId) {
        // Llama al método del repositorio para filtrar por el ID del usuario
        return companyRepository.findByUser_Id(userId);
    }

    public Company getCompanyById(Integer id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrado con ID: " + id));
    }

    /**
     * Verifica si un usuario tiene permiso para acceder a una empresa específica.
     * Esta lógica es crucial para el nuevo endpoint /select-company.
     * Contiene la regla especial: si el usuario es administrador (role=true), siempre tiene permiso.
     *
     * @param userId El ID del usuario que intenta acceder.
     * @param companyId El ID de la empresa a la que se intenta acceder.
     * @return true si el usuario tiene permiso, false en caso contrario.
     */
    public boolean isUserAssignedToCompany(Integer userId, Integer companyId) {
        // Obtenemos el usuario de la base de datos para verificar su rol.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario con ID " + userId + " no encontrado durante la verificación de permisos."));

        // REGLA CLAVE: Si el usuario tiene el rol de administrador, se le concede acceso automáticamente.
        if (user.getRole()) {
            return true;
        }

        // Si no es un administrador, aplicamos la regla estándar:
        // verificamos si existe una asignación directa en la base de datos.
        return companyRepository.existsByIdAndUser_Id(companyId, userId);
    }
}
