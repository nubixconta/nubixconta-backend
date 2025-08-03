package com.nubixconta.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JwtFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Permitir directamente las peticiones OPTIONS (preflight CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.replace("Bearer ", "");
            try {
                Claims claims = JwtUtil.getClaims(token);
                String userName = claims.getSubject();
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userName, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Después de autenticar al usuario, intentamos obtener el company_id del token.
                // El método .ifPresent() ejecutará el código solo si el Optional no está vacío.
                // Si el token es genérico (sin company_id), esta línea simplemente no hará nada.
                JwtUtil.extractCompanyId(token).ifPresent(TenantContext::setCurrentTenant);

            } catch (Exception e) {
                // Buena práctica: si el token es inválido, asegurarse de que el contexto de seguridad esté limpio.
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

        }

        try {
            // Permite que la petición continúe hacia el controlador.
            filterChain.doFilter(request, response);
        } finally {
            // CRÍTICO: Al final de la petición (incluso si hubo un error),
            // limpiamos el TenantContext. Esto previene que el company_id
            // se filtre a la siguiente petición que podría ser de otro usuario.
            TenantContext.clear();
        }
    }

}