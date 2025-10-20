package com.nubixconta.modules.administration.controller;
import com.nubixconta.modules.administration.dto.company.CompanyCreateDTO;
import com.nubixconta.modules.administration.dto.company.CompanyResponseDTO;
import com.nubixconta.modules.administration.dto.company.CompanyUpdateDTO;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.service.CompanyService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final ModelMapper modelMapper;


    @Autowired
    public CompanyController(CompanyService companyService,ModelMapper modelMapper) {
        this.companyService = companyService;
        this.modelMapper = modelMapper;
    }

    @PostMapping
    public ResponseEntity<CompanyResponseDTO> createCompany(
            @Valid @RequestBody CompanyCreateDTO dto) {

        Company saved = companyService.saveCompany(dto);
        CompanyResponseDTO response = modelMapper.map(saved, CompanyResponseDTO.class);

        return ResponseEntity.status(201).body(response);

    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Integer id) {
        try {
            Company company = companyService.getCompanyById(id);
            CompanyResponseDTO companyDTO = modelMapper.map(company, CompanyResponseDTO.class);
            return ResponseEntity.ok(companyDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }


    //Controlador para listar todas las empresas activas
    @GetMapping("/active") // Empresas con companyStatus = true
    public ResponseEntity<List<CompanyResponseDTO>> getAssignedCompanies() {
        List<CompanyResponseDTO> companies = companyService.getCompaniesByStatus(true);
        return ResponseEntity.ok(companies);
    }
    //Controlador para listar todas las empreas inactivas
    @GetMapping("/inactive") // Empresas con companyStatus = false
    public ResponseEntity<List<CompanyResponseDTO>> getUnassignedCompanies() {
        List<CompanyResponseDTO> companies = companyService.getCompaniesByStatus(false);
        return ResponseEntity.ok(companies);
    }

    //Controlador para listar todas las empresas activas y asignadas
    @GetMapping("/active-assigned") // Empresas con companyStatus = true
    public ResponseEntity<List<CompanyResponseDTO>> getCompaniesActiveAndAssigned() {
        List<CompanyResponseDTO> companies = companyService.getCompaniesByActiveAndAssigned(true,true);
        return ResponseEntity.ok(companies);
    }
    //Metodo para filtrar por nombre del usuario de la empresa y por el estatus de la empresa
    @GetMapping("/search")
    public ResponseEntity<List<Company>> searchCompanies(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Boolean status
    ) {

        List<Company> results = companyService.searchCompanies(companyName, userName, status);
        return ResponseEntity.ok(results);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Company> patchCompany(
            @PathVariable Integer id,
            @Valid @RequestBody CompanyUpdateDTO dto) {
        Company updated = companyService.patchCompany(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/byUser")
    public ResponseEntity<List<Company>> getMyCompanies(Authentication authentication) {
        String userName = authentication.getName();
        List<Company> companies = companyService.getCompaniesByUserName(userName);
        return ResponseEntity.ok(companies);
    }
    //Metodo para filtrar empresas por id del usuario
    @GetMapping("/byUser/{userId}")
    public ResponseEntity<List<Company>> getCompaniesByUserId(@PathVariable Integer userId) {
        List<Company> companies = companyService.getCompaniesByUserId(userId);
        return ResponseEntity.ok(companies);
    }
}
