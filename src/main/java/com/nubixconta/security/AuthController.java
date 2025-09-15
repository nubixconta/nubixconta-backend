package com.nubixconta.security;

import com.nubixconta.modules.administration.dto.AccessLog.AccessLogResponseDTO;
import com.nubixconta.modules.administration.dto.AccessLog.AccessLogUpdateDTO;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
import com.nubixconta.modules.administration.service.AccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.nubixconta.modules.administration.service.CompanyService;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private AccessLogService accessLogService;

    @Autowired
    private CompanyService companyService; // Inyectamos el servicio de empresas para la validación.

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginUser) {
        var userOpt = userRepository.findByUserName(loginUser.getUserName());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Usuario incorrecto");
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(loginUser.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Contraseña incorrecta");
        }

        String token = JwtUtil.generateToken(user);

        //En esta parte se encarga de almacenar el accessLogId
        AccessLogResponseDTO logResponse = accessLogService.recordLogin(user.getId());
        // Extraer el ID del AccessLog del DTO
        Long accessLogId = logResponse.getId();

       // Obtener el rol del usuario
        boolean userRole = user.getRole();
        // Regresa JSON plano (token y rol)
        String body = String.format("{ \"token\": \"%s\", \"role\": %b, \"accessLogId\": %d }",
                token, userRole, accessLogId);
        return ResponseEntity.ok().body(body);
    }
    // =========================================================================================
    // == INICIO DE CÓDIGO AÑADIDO: Endpoint para seleccionar empresa
    // =========================================================================================
    /**
     * NUEVO ENDPOINT para que el frontend obtenga un token con el scope de una empresa.
     * Es llamado DESPUÉS del login, cuando el usuario (asistente o admin) selecciona una empresa de la lista.
     *
     * @param companyId El ID de la empresa que el usuario ha seleccionado.
     * @return Una respuesta con un nuevo token JWT que incluye el company_id.
     */
    @PostMapping("/select-company/{companyId}")
    public ResponseEntity<?> selectCompany(@PathVariable Integer companyId) {
        try {
            // 1. Obtenemos el ID del usuario del token GENÉRICO que el frontend envía. Es seguro.
            Integer currentUserId = JwtUtil.extractCurrentUserId();

            // 2. Usamos nuestro nuevo servicio para verificar si este usuario tiene permiso sobre la empresa.
            //    Esta llamada ya maneja la lógica de que el admin siempre tiene permiso.
            if (!companyService.isUserAssignedToCompany(currentUserId, companyId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso a la empresa no autorizado.");
            }

            // 3. Obtenemos el objeto User completo para generar el token.
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado al generar token de empresa."));

            // 4. Generamos un NUEVO token, esta vez pasándole el companyId.
            String scopedToken = JwtUtil.generateToken(user, companyId);

            // 5. Devolvemos el nuevo token al frontend.
            String responseBody = String.format("{ \"token\": \"%s\" }", scopedToken);
            return ResponseEntity.ok(responseBody);

        } catch (RuntimeException e) {
            // Captura errores como "token no encontrado" o "usuario no encontrado".
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
    // =========================================================================================
    // == FIN DE CÓDIGO AÑADIDO
    // =========================================================================================


    //Metodo para registrar el cierre de session para la bitacoras de acceso
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody AccessLogUpdateDTO accessLogUpdateDTO) {
        Integer currentUserId = JwtUtil.extractCurrentUserId();

        try {
            accessLogService.recordLogout(accessLogUpdateDTO.getAccessLogId(), currentUserId);
            return ResponseEntity.ok().body("Sesión cerrada exitosamente.");
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}