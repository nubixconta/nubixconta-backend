package com.nubixconta.modules.administration.service;


import com.nubixconta.modules.administration.dto.company.CompanyResponseDTO;
import com.nubixconta.modules.administration.dto.user.*;
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

        // Validar unicidad del userName
        if (userRepository.existsByUserName(userdto.getUserName())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        if (userRepository.existsByFirstNameAndLastName(userdto.getFirstName(), userdto.getLastName())) {
            throw new IllegalArgumentException("Ya existe un usuario con el mismo nombre y apellido.");
        }

        // --- Lógica de roles y límites ---
        long totalUsers = userRepository.count();
        boolean isAdminUser = false; // Por defecto, no es admin

        if (totalUsers == 0) {
            // Si no hay usuarios, este será el administrador
            isAdminUser = true;
        } else {
            // Si ya hay al menos un usuario (potencialmente el admin),
            // verificamos el límite de asistentes
            long nonAdminUserCount = userRepository.countByRole(false);
            if (nonAdminUserCount >= 5) {
                throw new IllegalStateException("Se ha alcanzado el número máximo de usuarios asistentes (5).");
            }
            // Si no es el primer usuario y no se ha alcanzado el límite de asistentes,
            // este usuario será un asistente (role = false)
            isAdminUser = false;
        }
        // --- Fin de la lógica de roles y límites ---

        User user = modelMapper.map(userdto, User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(isAdminUser);
        User saved = userRepository.save(user);

        // Solo registrar bitácora si NO es el primer usuario
        if (userRepository.count() > 1) {
            changeHistoryService.logChange(
                    "Administración",
                    "Se creó el usuario " + saved.getFirstName() + " " + saved.getLastName()
            );
        }

        return saved;
    }
    /**
     * Permite al administrador cambiar su propia contraseña.
     */
    @Transactional
    public User changeAdminPassword(Integer adminId, String oldPassword, String newPassword) {
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado."));

        // 1. Validar la contraseña antigua del administrador
        if (!passwordEncoder.matches(oldPassword, adminUser.getPassword())) {
            throw new IllegalArgumentException("La contraseña anterior es incorrecta.");
        }

        // 2. Validar que la nueva contraseña cumpla con los requisitos de seguridad
        if (!isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("La nueva contraseña debe contener al menos 8 caracteres, una mayúscula, un número y un símbolo.");
        }

        // 3. Codificar y guardar la nueva contraseña
        adminUser.setPassword(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(adminUser);

        // 4. Registrar el cambio en la bitácora
        changeHistoryService.logChange(
                "Administración",
                "La contraseña del administrador " + adminUser.getUserName() + " fue actualizada."
        );

        return updatedUser;
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


        User saved = userRepository.save(user);

        if (!cambios.isEmpty()) {
            changeHistoryService.logChange(
                    "Administración",
                    cambios.toString()
            );
        }

        return saved;
    }

    /**
     * Permite al administrador cambiar su propia contraseña.
     */
    @Transactional
    public User changeAdminPassword(Integer adminId, ChangePasswordDTO dto) {
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado."));

        if (!passwordEncoder.matches(dto.getOldPassword(), adminUser.getPassword())) {
            throw new IllegalArgumentException("La contraseña anterior es incorrecta.");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("La nueva contraseña y su confirmación no coinciden.");
        }

        if (!isStrongPassword(dto.getNewPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe contener al menos 8 caracteres, una mayúscula, una minúscula, un número y un símbolo.");
        }

        adminUser.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        User updatedUser = userRepository.save(adminUser);

        changeHistoryService.logChange(
                "Administración",
                "La contraseña del administrador " + adminUser.getUserName() + " fue actualizada."
        );

        return updatedUser;
    }

    /**
     * Permite al administrador cambiar la contraseña de un asistente.
     */
    @Transactional
    public User resetUserPasswordByAdmin(Integer userId, Integer adminId, AdminResetPasswordDTO dto) {
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado."));

        if (!adminUser.getRole()) { // Valida si el usuario autenticado es realmente un administrador
            throw new SecurityException("Permiso denegado. Solo los administradores pueden realizar esta acción.");
        }

        if (!passwordEncoder.matches(dto.getAdminPassword(), adminUser.getPassword())) {
            throw new IllegalArgumentException("Contraseña de administrador incorrecta.");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("La nueva contraseña y su confirmación no coinciden.");
        }

        if (!isStrongPassword(dto.getNewPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe contener al menos 8 caracteres, una mayúscula, una minúscula, un número y un símbolo.");
        }

        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario asistente no encontrado."));

        userToUpdate.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        User updatedUser = userRepository.save(userToUpdate);

        changeHistoryService.logChange(
                "Administración",
                "El administrador " + adminUser.getUserName() + " actualizó la contraseña del usuario " + userToUpdate.getUserName() + "."
        );

        return updatedUser;
    }


    /**
     * Método auxiliar para validar la fortaleza de la contraseña.
     */
    private boolean isStrongPassword(String password) {
        // Expresión regular para validar: al menos 8 caracteres, 1 mayúscula, 1 minúscula, 1 número, 1 símbolo.
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,100}$";
        return password.matches(regex);
    }



    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    //Trae a todos los usuarios asistentes
    public List<UserResponseDTO> getUserByAssistant(boolean role, Boolean status) {
        List<User> users = userRepository.findByRoleAndStatus(role,status);
        return users.stream()
                .map(user -> modelMapper.map(user, UserResponseDTO.class))
                .toList();
    }
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

}
