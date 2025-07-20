package com.nubixconta.modules.administration.controller;

import com.nubixconta.modules.administration.dto.company.CompanyResponseDTO;
import com.nubixconta.modules.administration.dto.user.UserCreateDTO;
import com.nubixconta.modules.administration.dto.user.UserResponseDTO;
import com.nubixconta.modules.administration.dto.user.UserUpdateDTO;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.service.UserService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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



    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
//Metodo que lista a asistentes contables
    @GetMapping("/assistant")
    public ResponseEntity<List<UserResponseDTO>> getUserByAssistant() {
        List<UserResponseDTO> users = userService.getUserByAssistant(false);
        return ResponseEntity.ok(users);
    }

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