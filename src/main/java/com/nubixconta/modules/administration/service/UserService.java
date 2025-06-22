package com.nubixconta.modules.administration.service;


import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    @Autowired
    private ChangeHistoryService changeHistoryService;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public User saveUser(User user) {
        User savedUser = userRepository.save(user);

        // Crear registro de auditoría con company_id en null
        String action = "Se registró el usuario " + savedUser.getFirstName() + " " + savedUser.getLastName();
        changeHistoryService.logChange("Administración", action, savedUser.getId(), null);
        // <== companyId null

        return savedUser;
    }

    public User updateUser(Integer id, User updatedUser) {
        return userRepository.findById(id).map(existingUser -> {

            if (!updatedUser.getRole().equals(existingUser.getRole())) {
                throw new RuntimeException("No está permitido modificar el rol del usuario.");
            }

            //  Actualizar solo los campos permitidos
            existingUser.setFirstName(updatedUser.getFirstName());
            existingUser.setLastName(updatedUser.getLastName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setPassword(updatedUser.getPassword());
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
