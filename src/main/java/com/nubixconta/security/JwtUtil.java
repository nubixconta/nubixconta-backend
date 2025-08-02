package com.nubixconta.security;

import com.nubixconta.modules.administration.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Optional;

import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET_KEY = "clave-super-secreta-clave-super-secreta-2024-xxx"; // ¡mínimo 32 caracteres!
    private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24h

    private static Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // =========================================================================================
    // == INICIO DE CÓDIGO MODIFICADO Y AÑADIDO
    // =========================================================================================

    /**
     * MÉTODO ORIGINAL MODIFICADO: Ahora delega la creación al nuevo método sobrecargado.
     * Esto asegura que el código antiguo que llama a generateToken(user) siga funcionando
     * y genere un token genérico sin company_id.
     * @param user El objeto User para generar el token.
     * @return Un token JWT genérico.
     */
    public static String generateToken(User user) {
        // Llama al nuevo método principal pasando null para el companyId.
        return generateToken(user, null);
    }

    /**
     * NUEVO MÉTODO SOBRECARGADO: Es el nuevo método principal para generar todos los tokens.
     * Si se proporciona un companyId, lo añade como un "claim" al token.
     * Si companyId es null, crea un token genérico (comportamiento original).
     *
     * @param user El objeto User.
     * @param companyId El ID de la empresa a incluir en el token, o null.
     * @return Un token JWT, posiblemente con el scope de una empresa.
     */
    public static String generateToken(User user, Integer companyId) {
        // Construye la base del token.
        var tokenBuilder = Jwts.builder()
                .setSubject(user.getUserName())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION));

        // Lógica condicional: Si se nos da un companyId, lo añadimos al token.
        if (companyId != null) {
            tokenBuilder.claim("company_id", companyId);
        }

        // Firma y compacta el token.
        return tokenBuilder.signWith(getSigningKey()).compact();
    }


    /**
     * NUEVO MÉTODO: Extrae de forma segura el 'company_id' de un token.
     * Será utilizado por el JwtFilter para establecer el TenantContext.
     *
     * @param token El token JWT completo (sin "Bearer ").
     * @return un Optional que contiene el companyId si existe, o un Optional vacío.
     */
    public static Optional<Integer> extractCompanyId(String token) {
        try {
            Claims claims = getClaims(token);
            // El segundo parámetro de .get() es el tipo esperado. Esto evita casteos manuales.
            Integer companyId = claims.get("company_id", Integer.class);
            return Optional.ofNullable(companyId);
        } catch (Exception e) {
            // Si el claim no existe, o hay cualquier otro error al parsear, devolvemos un Optional vacío.
            return Optional.empty();
        }
    }

    // =========================================================================================
    // == FIN DE CÓDIGO MODIFICADO Y AÑADIDO
    // =========================================================================================

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