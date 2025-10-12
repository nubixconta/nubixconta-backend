package com.nubixconta.modules.administration.service;

import com.nubixconta.modules.administration.dto.company.CompanyCreateDTO;
import com.nubixconta.modules.administration.dto.company.CompanyResponseDTO;
import com.nubixconta.modules.administration.dto.company.CompanyUpdateDTO;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.modules.administration.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio CompanyService.
 * Se realizan con JUnit5 y Mockito, sin conexión a base de datos.
 * 
 * Cubre HU-ADM-001 (Registrar), HU-ADM-003 (Listar), 
 * HU-ADM-004 (Editar) y HU-ADM-005 (Desactivar empresa).
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    // ================== Dependencias simuladas ==================
    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ChangeHistoryService changeHistoryService;

    // Inyección de mocks al servicio
    @InjectMocks
    private CompanyService companyService;

    // ================== Configuración inicial ==================
    @BeforeEach
    void setUp() {
        // No es necesario inicializar manualmente, MockitoExtension lo hace.
    }

    // =============================================================
    // HU-ADM-001: Registrar empresa nueva
    // =============================================================

    @Test
    void saveCompany_deberiaGuardarEmpresaCorrectamente() {
        // Arrange: datos de prueba
        CompanyCreateDTO dto = new CompanyCreateDTO();
        dto.setCompanyName("Mi Empresa");
        dto.setCompanyNit("0614-123456-001-7");

        Company empresaGuardada = new Company();
        empresaGuardada.setCompanyName("Mi Empresa");
        empresaGuardada.setCompanyNit("0614-123456-001-7");

        // Configurar comportamiento simulado
        when(companyRepository.existsByCompanyName(dto.getCompanyName())).thenReturn(false);
        when(companyRepository.existsByCompanyNit(dto.getCompanyNit())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(empresaGuardada);

        // Act
        Company resultado = companyService.saveCompany(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals("Mi Empresa", resultado.getCompanyName());
        verify(companyRepository, times(1)).save(any(Company.class));
        verify(changeHistoryService, times(1))
                .logChange(eq("Administración"), contains("Se creó la empresa"));
    }

    @Test
    void saveCompany_deberiaLanzarErrorSiNombreDuplicado() {
        // Arrange
        CompanyCreateDTO dto = new CompanyCreateDTO();
        dto.setCompanyName("Mi Empresa");

        when(companyRepository.existsByCompanyName("Mi Empresa")).thenReturn(true);

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> companyService.saveCompany(dto));

        assertEquals("El nombre de empresa ya está registrado.", ex.getMessage());
        verify(companyRepository, never()).save(any());
    }

    // =============================================================
    // HU-ADM-003: Listar empresas
    // =============================================================

    @Test
    void getAllCompanies_deberiaRetornarListaDeEmpresas() {
        // Arrange
        Company c1 = new Company();
        c1.setCompanyName("Empresa 1");
        Company c2 = new Company();
        c2.setCompanyName("Empresa 2");

        when(companyRepository.findAll()).thenReturn(Arrays.asList(c1, c2));

        // Act
        List<CompanyResponseDTO> resultado = companyService.getAllCompanies();

        // Assert
        assertEquals(2, resultado.size());
        assertEquals("Empresa 1", resultado.get(0).getCompanyName());
        verify(companyRepository, times(1)).findAll();
    }

    // =============================================================
    // HU-ADM-004: Editar empresa
    // =============================================================

    @Test
    void patchCompany_deberiaActualizarNombreCorrectamente() {
        // Arrange
        Company existente = new Company();
        existente.setId(1);
        existente.setCompanyName("Antigua Empresa");

        CompanyUpdateDTO dto = new CompanyUpdateDTO();
        dto.setCompanyName("Nueva Empresa");

        when(companyRepository.findById(1)).thenReturn(Optional.of(existente));
        when(companyRepository.existsByCompanyName("Nueva Empresa")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Company resultado = companyService.patchCompany(1, dto);

        // Assert
        assertEquals("Nueva Empresa", resultado.getCompanyName());
        verify(changeHistoryService).logChange(eq("Administración"), contains("Nombre cambiado"));
    }

    @Test
    void patchCompany_deberiaLanzarErrorSiEmpresaNoExiste() {
        // Arrange
        when(companyRepository.findById(99)).thenReturn(Optional.empty());
        CompanyUpdateDTO dto = new CompanyUpdateDTO();

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyService.patchCompany(99, dto));

        assertTrue(ex.getMessage().contains("Empresa no encontrada"));
    }

    // =============================================================
    // HU-ADM-005: Desactivar empresa
    // =============================================================

    @Test
    void patchCompany_deberiaDesactivarEmpresaCorrectamente() {
        // Arrange
        Company empresa = new Company();
        empresa.setId(1);
        empresa.setActiveStatus(true);

        CompanyUpdateDTO dto = new CompanyUpdateDTO();
        dto.setActiveStatus(false);

        when(companyRepository.findById(1)).thenReturn(Optional.of(empresa));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Company resultado = companyService.patchCompany(1, dto);

        // Assert
        assertFalse(resultado.getActiveStatus());
        verify(changeHistoryService).logChange(eq("Administración"), contains("actividad de la empresa cambió"));
    }

    // =============================================================
    // HU-ADM-002 (soporte): Verificar asignación de empresas
    // =============================================================

    @Test
    void isUserAssignedToCompany_adminSiempreTieneAcceso() {
        // Arrange
        User admin = new User();
        admin.setId(1);
        admin.setRole(true); // Administrador

        when(userRepository.findById(1)).thenReturn(Optional.of(admin));

        // Act
        boolean resultado = companyService.isUserAssignedToCompany(1, 10);

        // Assert
        assertTrue(resultado);
        verify(companyRepository, never()).existsByIdAndUser_Id(any(), any());
    }

    @Test
    void isUserAssignedToCompany_usuarioComunSoloSiAsignado() {
        // Arrange
        User user = new User();
        user.setId(2);
        user.setRole(false);

        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(companyRepository.existsByIdAndUser_Id(10, 2)).thenReturn(true);

        // Act
        boolean resultado = companyService.isUserAssignedToCompany(2, 10);

        // Assert
        assertTrue(resultado);
        verify(companyRepository).existsByIdAndUser_Id(10, 2);
    }
}
