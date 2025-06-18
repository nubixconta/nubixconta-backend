package com.nubixconta.modules.administration.controller;
import com.nubixconta.modules.administration.service.AdministrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/administration")
public class AdministrationController {
    @Autowired
    private AdministrationService administrationService;

    // TODO: Agregar endpoints REST para administración
}
