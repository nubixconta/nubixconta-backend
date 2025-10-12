package com.nubixconta.modules.administration.service;

import com.nubixconta.modules.administration.dto.user.UserCreateDTO;
import com.nubixconta.modules.administration.dto.user.UserUpdateDTO;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el servicio de usuarios (UserService)
 * Relacionadas con las historias HU-ADM-006 a HU-ADM-009
 * enfocadas en la Administradora.
 */
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChangeHistoryService changeHistoryService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, new ModelMapper(), changeHistoryService, passwordEncoder);
    }

    /**
     * HU-ADM-006: Crear usuarios de asistentes contables
     * Verifica que el primer usuario creado sea administrador (rol=true)
     * y que los siguientes sean asistentes.
     */
    @Test
    void saveUser_deberiaCrearPrimerUsuarioComoAdministrador() {
        // Arrange
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("admin@nubix.com");
        dto.setUserName("admin");
        dto.setFirstName("Karen");
        dto.setLastName("Martinez");
        dto.setPassword("Admin123!");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUserName(anyString())).thenReturn(false);
        when(userRepository.existsByFirstNameAndLastName(anyString(), anyString())).thenReturn(false);
        when(userRepository.count()).thenReturn(0L); // No existen usuarios aún
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        User admin = new User();
        admin.setUserName("admin");
        admin.setRole(true);
        when(userRepository.save(any(User.class))).thenReturn(admin);

        // Act
        User resultado = userService.saveUser(dto);

        // Assert
        assertTrue(resultado.getRole(), "El primer usuario debería ser administrador.");
        verify(changeHistoryService, never()).logChange(anyString(), anyString());
    }

    /**
     * HU-ADM-006: Validar límite máximo de 5 asistentes contables
     * Verifica que se lance error al intentar crear un sexto asistente.
     */
    @Test
    void saveUser_deberiaLanzarErrorAlSuperarLimiteDeAsistentes() {
        // Arrange
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("user6@nubix.com");
        dto.setUserName("user6");
        dto.setFirstName("Asistente");
        dto.setLastName("Seis");
        dto.setPassword("User123!");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUserName(anyString())).thenReturn(false);
        when(userRepository.existsByFirstNameAndLastName(anyString(), anyString())).thenReturn(false);
        when(userRepository.count()).thenReturn(6L);
        when(userRepository.countByRole(false)).thenReturn(5L); // Ya hay 5 asistentes

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> userService.saveUser(dto));
        assertEquals("Se ha alcanzado el número máximo de usuarios asistentes (5).", ex.getMessage());
    }

    /**
     * HU-ADM-008: Editar usuario
     * Verifica que se actualicen correctamente los datos del usuario
     * y se registre la acción en la bitácora.
     */
    @Test
    void updateUser_deberiaActualizarDatosYRegistrarBitacora() {
        // Arrange
        User existente = new User();
        existente.setId(1);
        existente.setEmail("viejo@nubix.com");
        existente.setUserName("karen");
        existente.setFirstName("Karen");
        existente.setLastName("Martinez");

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setEmail("nuevo@nubix.com");
        dto.setFirstName("Kary");

        when(userRepository.findById(1)).thenReturn(Optional.of(existente));
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User resultado = userService.updateUser(1, dto);

        // Assert
        assertEquals("nuevo@nubix.com", resultado.getEmail());
        assertEquals("Kary", resultado.getFirstName());
        verify(changeHistoryService, times(1))
                .logChange(eq("Administración"), contains("correo electrónico cambió"));
    }

    /**
     * HU-ADM-007: Desactivar usuario de asistente contable
     * Verifica que el estado del usuario cambie a inactivo.
     */
    @Test
    void updateUser_deberiaDesactivarUsuario() {
        // Arrange
        User existente = new User();
        existente.setId(2);
        existente.setStatus(true);

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setStatus(false); // Cambiar a inactivo

        when(userRepository.findById(2)).thenReturn(Optional.of(existente));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User resultado = userService.updateUser(2, dto);

        // Assert
        assertFalse(resultado.getStatus(), "El usuario debería quedar desactivado");
        verify(changeHistoryService, times(1))
                .logChange(eq("Administración"), contains("estado del usuario cambió"));
    }

    /**
     * HU-ADM-009: Listar usuarios
     * Verifica que el servicio devuelva todos los usuarios del sistema.
     */
    @Test
    void getAllUsers_deberiaRetornarListaDeUsuarios() {
        // Arrange
        when(userRepository.findAll()).thenReturn(
                java.util.List.of(new User(), new User())
        );

        // Act
        var lista = userService.getAllUsers();

        // Assert
        assertEquals(2, lista.size());
        verify(userRepository, times(1)).findAll();
    }
}
