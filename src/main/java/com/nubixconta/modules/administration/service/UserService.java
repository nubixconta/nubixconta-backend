package com.nubixconta.modules.administration.service;


import com.nubixconta.modules.administration.dto.user.UserCreateDTO;
import com.nubixconta.modules.administration.dto.user.UserUpdateDTO;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
        // Bitácora
        changeHistoryService.logChange(
                "Administración",
                "Se creó el usuario " + saved.getFirstName() + " " + saved.getLastName(),
                null
        );
        return saved;

    }

    @Transactional
    public User updateUser(Integer id, UserUpdateDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        modelMapper.map(dto, user);

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return userRepository.save(user);
    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

}
