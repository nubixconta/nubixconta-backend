package com.nubixconta.security;

import com.nubixconta.modules.administration.dto.AccessLog.AccessLogResponseDTO;
import com.nubixconta.modules.administration.dto.AccessLog.AccessLogUpdateDTO;
import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
import com.nubixconta.modules.administration.service.AccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private AccessLogService accessLogService;

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