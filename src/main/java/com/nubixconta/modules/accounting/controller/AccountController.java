package com.nubixconta.modules.accounting.controller;

import com.nubixconta.modules.accounting.dto.Account.AccountBankResponseDTO;
import com.nubixconta.modules.accounting.dto.catalog.MasterAccountNodeDTO;
import com.nubixconta.modules.accounting.service.CollectionEntryService;
import com.nubixconta.modules.accounting.service.MasterAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CollectionEntryService collectionEntryService;
    private final MasterAccountService masterAccountService;

    // Modificar el constructor para inyectar el nuevo servicio
    public AccountController(CollectionEntryService collectionEntryService, MasterAccountService masterAccountService) {
        this.collectionEntryService = collectionEntryService;
        this.masterAccountService = masterAccountService;
    }

    //Metodo para filtrar solo cuentas bancarias
    @GetMapping("/bank-accounts")
    public ResponseEntity<List<AccountBankResponseDTO>> getBankAccounts() {
        return ResponseEntity.ok(collectionEntryService.findBankAccounts());
    }
    /**
     * Devuelve la estructura de árbol completa del Catálogo Maestro global.
     * Usado por el frontend para mostrar las cuentas que una empresa puede activar.
     */
    @GetMapping("/master/tree")
    public ResponseEntity<List<MasterAccountNodeDTO>> getMasterAccountTree() {
        return ResponseEntity.ok(masterAccountService.getTree());
    }
   /** Carga y reemplaza el Catálogo Maestro global desde un archivo Excel.
    Requiere rol de SUPER_ADMIN (a configurar en Spring Security).
            */
    @PostMapping("/master/upload")
//@PreAuthorize("hasRole('SUPER_ADMIN')") // Descomentar cuando tengas roles
    public ResponseEntity<String> uploadMasterCatalog(@RequestParam("file") MultipartFile file) {
        try {
            masterAccountService.processMasterCatalogUpload(file);
            return ResponseEntity.ok("Catálogo Maestro cargado exitosamente.");
        } catch (Exception e) {
// En una app real, tendrías un manejo de errores más específico
            return ResponseEntity.badRequest().body("Error al procesar el archivo: " + e.getMessage());
        }
    }
}
