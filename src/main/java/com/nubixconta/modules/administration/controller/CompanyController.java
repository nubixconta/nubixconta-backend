package com.nubixconta.modules.administration.controller;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.service.CompanyService;
import jakarta.validation.Valid;
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

    @Autowired
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public ResponseEntity<Company> createCompany(@Valid @RequestBody Company company) {
        Company saved = companyService.saveCompany(company);
        return ResponseEntity.status(201).body(saved);
    }
    @GetMapping
    public ResponseEntity<List<Company>> getAllCompanies() {
        List<Company> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }
    //Metodo para filtrar por nombre del usuario de la empresa y por el estatus de la empresa
    @GetMapping("/search")
    public ResponseEntity<List<Company>> searchCompanies(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Boolean status
    ) {
        System.out.println("companyName = " + companyName);
        System.out.println("userName = " + userName);
        System.out.println("status = " + status);

        List<Company> results = companyService.searchCompanies(companyName, userName, status);
        return ResponseEntity.ok(results);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Company> updateCompany(@PathVariable Integer id, @Valid @RequestBody Company company) {
        Company updated = companyService.updateCompany(id, company);
        return ResponseEntity.ok(updated);
    }
    @PatchMapping("/{id}")
    public ResponseEntity<Company> patchCompany(@PathVariable Integer id, @RequestBody Map<String, Object> updates) {
        Company updated = companyService.patchCompany(id, updates);
        return ResponseEntity.ok(updated);
    }
    @GetMapping("/byUser")
    public ResponseEntity<List<Company>> getMyCompanies(Authentication authentication) {
        String userName = authentication.getName();
        List<Company> companies = companyService.getCompaniesByUserName(userName);
        return ResponseEntity.ok(companies);
    }
}

