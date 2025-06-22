package com.nubixconta.modules.administration.service;
import com.nubixconta.modules.administration.repository.AdministrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdministrationService {
    @Autowired
    private AdministrationRepository administrationRepository;

    // TODO: Agregar lógica de negocio para administración
}
