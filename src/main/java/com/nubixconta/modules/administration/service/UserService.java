package com.nubixconta.modules.administration.service;


import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    @Autowired
    private ChangeHistoryService changeHistoryService;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        String action = "Se registró el usuario " + savedUser.getFirstName() + " " + savedUser.getLastName();
        changeHistoryService.logChange("Administración", action, savedUser.getId(), null);
        return savedUser;
    }

    public User updateUser(Integer id, User updatedUser) {
        return userRepository.findById(id).map(existingUser -> {
            if (!updatedUser.getRole().equals(existingUser.getRole())) {
                throw new RuntimeException("No está permitido modificar el rol del usuario.");
            }
            existingUser.setFirstName(updatedUser.getFirstName());
            existingUser.setLastName(updatedUser.getLastName());
            existingUser.setEmail(updatedUser.getEmail());
            // Solo cifrar si la contraseña fue modificada
            if (!updatedUser.getPassword().equals(existingUser.getPassword())) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            existingUser.setPhoto(updatedUser.getPhoto());
            existingUser.setStatus(updatedUser.getStatus());
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

}
