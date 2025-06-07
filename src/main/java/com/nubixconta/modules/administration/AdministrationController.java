package com.nubixconta.modules.administration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/administration")
public class AdministrationController {
    @Autowired
    private AdministrationService administrationService;

    // TODO: Agregar endpoints REST para administración
}
