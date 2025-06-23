package com.nubixconta.security;

import com.nubixconta.modules.administration.entity.User;
import com.nubixconta.modules.administration.repository.UserRepository;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginUser) {
        var userOpt = userRepository.findByUserName(loginUser.getUserName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("UserName incorrecto");
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(loginUser.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Contraseña incorrecta");
        }
        String token = JwtUtil.generateToken(user);
        // Regresa JSON plano (token y rol)
        String body = "{ \"token\": \"" + token + "\", \"role\": " + user.getRole() + " }";
        return ResponseEntity.ok().body(body);
    }
}