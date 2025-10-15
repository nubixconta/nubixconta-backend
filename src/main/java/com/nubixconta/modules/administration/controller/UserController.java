package com.nubixconta.modules.administration.controller;

import com.nubixconta.modules.administration.dto.company.CompanyResponseDTO;
import com.nubixconta.modules.administration.dto.user.*;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.service.UserService;
import com.nubixconta.security.JwtUtil;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;
    @Autowired
    public UserController(UserService userService, ModelMapper modelMapper) {
        this.userService = userService;
        this.modelMapper = modelMapper;
    }


    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateDTO userDTO) {
        User savedUser = userService.saveUser(userDTO);
        UserResponseDTO responseDTO = modelMapper.map(savedUser, UserResponseDTO.class);
        return ResponseEntity.status(201).body(responseDTO);
    }

    @PatchMapping("/{id}")

    public ResponseEntity<?> patchUser(@PathVariable Integer id, @RequestBody UserUpdateDTO dto) {
        try {
            User updatedUser = userService.updateUser(id, dto);
            UserResponseDTO response = modelMapper.map(updatedUser, UserResponseDTO.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // muestra el verdadero error en consola
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // Endpoint para que el propio administrador cambie su contraseña
    @PatchMapping("/change-password/{id}")
    public ResponseEntity<?> changePassword(@PathVariable Integer id, @RequestBody ChangePasswordDTO dto) {
        try {
            /// Obtener el ID del usuario autenticado desde el token de forma segura
            Integer authenticatedUserId = JwtUtil.extractCurrentUserId();

            if (!authenticatedUserId.equals(id)) {
                return ResponseEntity.status(403).body("No tienes permiso para cambiar la contraseña de este usuario.");
            }
            // Si la validación pasa, llama al servicio con el ID obtenido del token
            userService.changeAdminPassword(id, dto);
            return ResponseEntity.ok("Contraseña actualizada exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    //Endpoin para modificarla contraseña de un usuario asistente
    @PatchMapping("/{userId}/reset-password")
    public ResponseEntity<?> resetPasswordForUser(@PathVariable Integer userId, @RequestBody AdminResetPasswordDTO dto) {
        try {
            // Obtener el ID del administrador autenticado desde el token de forma segura
            Integer adminId = JwtUtil.extractCurrentUserId();

            // Llama al servicio con el ID del usuario a modificar y el ID del administrador que lo autoriza
            userService.resetUserPasswordByAdmin(userId, adminId, dto);
            return ResponseEntity.ok("Contraseña del usuario reiniciada exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    //Metodo que lista a asistentes contables activos
    @GetMapping("/assistant")
    public ResponseEntity<List<UserResponseDTO>> getUserByAssistant() {
        List<UserResponseDTO> users = userService.getUserByAssistant(false,true);
        return ResponseEntity.ok(users);
    }
    //Enpoint para obtener un usuario por su id
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

}