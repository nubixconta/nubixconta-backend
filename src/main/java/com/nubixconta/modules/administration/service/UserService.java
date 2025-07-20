package com.nubixconta.modules.administration.service;


import com.nubixconta.modules.administration.dto.company.CompanyResponseDTO;
import com.nubixconta.modules.administration.dto.user.UserCreateDTO;
import com.nubixconta.modules.administration.dto.user.UserResponseDTO;
import com.nubixconta.modules.administration.dto.user.UserUpdateDTO;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private   ChangeHistoryService changeHistoryService;
    private final ModelMapper modelMapper;
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,ModelMapper modelMapper,
                       ChangeHistoryService changeHistoryService,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.changeHistoryService = changeHistoryService;
        this.passwordEncoder = passwordEncoder;


    }


    public User saveUser(UserCreateDTO userdto) {
        // Validar unicidad del correo
        if (userRepository.existsByEmail(userdto.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }

        User user = modelMapper.map(userdto, User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User saved = userRepository.save(user);

        // Solo registrar bitácora si NO es el primer usuario
        if (userRepository.count() > 1) {
            changeHistoryService.logChange(
                    "Administración",
                    "Se creó el usuario " + saved.getFirstName() + " " + saved.getLastName(),
                    null
            );
        }

        return saved;
    }


    @Transactional
    public User updateUser(Integer id, UserUpdateDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        StringBuilder cambios = new StringBuilder();

        // Validar email
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El correo electrónico ya está registrado.");
            }
            cambios.append("El correo electrónico cambió de ")
                    .append(user.getEmail()).append(" a ").append(dto.getEmail()).append(". ");
            user.setEmail(dto.getEmail());
        }

        // Validar username
        if (dto.getUserName() != null && !dto.getUserName().equals(user.getUserName())) {
            if (userRepository.existsByUserName(dto.getUserName())) {
                throw new IllegalArgumentException("El userName ya está registrado.");
            }
            cambios.append("El userName cambió de ")
                    .append(user.getUserName()).append(" a ").append(dto.getUserName()).append(". ");
            user.setUserName(dto.getUserName());
        }

        // Otros campos opcionales
        if (dto.getFirstName() != null && !dto.getFirstName().equals(user.getFirstName())) {
            cambios.append("El nombre cambió de ")
                    .append(user.getFirstName()).append(" a ").append(dto.getFirstName()).append(". ");
            user.setFirstName(dto.getFirstName());
        }

        if (dto.getLastName() != null && !dto.getLastName().equals(user.getLastName())) {
            cambios.append("El apellido cambió de ")
                    .append(user.getLastName()).append(" a ").append(dto.getLastName()).append(". ");
            user.setLastName(dto.getLastName());
        }

        if (dto.getPhoto() != null && !dto.getPhoto().equals(user.getPhoto())) {
            cambios.append("La URL de la foto cambió. ");
            user.setPhoto(dto.getPhoto());
        }

        if (dto.getStatus() != null && !dto.getStatus().equals(user.getStatus())) {
            String estadoAnterior = user.getStatus() ? "activo" : "inactivo";
            String estadoNuevo = dto.getStatus() ? "activo" : "inactivo";
            cambios.append("El estado del usuario cambió de ")
                    .append(estadoAnterior).append(" a ").append(estadoNuevo).append(". ");
            user.setStatus(dto.getStatus());
        }

        // Contraseña
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            cambios.append("La contraseña fue actualizada. ");
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User saved = userRepository.save(user);

        if (!cambios.isEmpty()) {
            changeHistoryService.logChange(
                    "Administración",
                    cambios.toString(),
                    null // Si deseas relacionarlo con alguna empresa, puedes pasar el ID aquí
            );
        }

        return saved;
    }



    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    //Trae a todos los usuarios asistentes
    public List<UserResponseDTO> getUserByAssistant(boolean role) {
        List<User> users = userRepository.findByRole(role);
        return users.stream()
                .map(user -> modelMapper.map(user, UserResponseDTO.class))
                .toList();
    }
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

}
