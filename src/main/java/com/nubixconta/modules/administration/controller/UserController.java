package com.nubixconta.modules.administration.controller;

import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /* -------------------- CREAR -------------------- */
    @PostMapping
    public ResponseEntity<User> createUser(
            @ModelAttribute User user,                            //  <—  SIN @Valid
            @RequestParam(value = "file", required = false) MultipartFile file) {

        // valores por defecto (antes de guardar)
        user.setStatus(Boolean.TRUE);
        user.setRole(Boolean.FALSE);

        handleFileUpload(user, file);
        return ResponseEntity.status(201).body(userService.saveUser(user));
    }

    /* -------------------- ACTUALIZAR -------------------- */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Integer id,
            @ModelAttribute User user,                            //  <—  SIN @Valid
            @RequestParam(value = "file", required = false) MultipartFile file) {

        handleFileUpload(user, file);
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    /* -------------------- LISTAR / OBTENER -------------------- */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(ex.getMessage());
        }
    }

    /* -------------------- PRIVADO -------------------- */
    private void handleFileUpload(User user, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path uploadDir = Paths.get("src/main/resources/static/uploads/");
                Files.createDirectories(uploadDir);
                Files.copy(file.getInputStream(),
                           uploadDir.resolve(filename),
                           StandardCopyOption.REPLACE_EXISTING);
                user.setPhoto("/uploads/" + filename);
            } catch (IOException e) {
                throw new RuntimeException("Error guardando la imagen", e);
            }
        }
    }
}
