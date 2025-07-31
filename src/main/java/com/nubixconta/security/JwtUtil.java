package com.nubixconta.security;

import com.nubixconta.modules.administration.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET_KEY = "clave-super-secreta-clave-super-secreta-2024-xxx"; // ¡mínimo 32 caracteres!
    private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24h

    private static Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUserName())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    public static Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public static Integer extractUserId(String token) {
        Claims claims = getClaims(token.replace("Bearer ", ""));
        return claims.get("userId", Integer.class);
    }
    public static Integer extractCurrentUserId() {
        // Obtiene el token del header "Authorization"
        String bearerToken = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest()
                .getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return extractUserId(bearerToken);
        }

        throw new RuntimeException("Token JWT no encontrado o inválido");
    }
}