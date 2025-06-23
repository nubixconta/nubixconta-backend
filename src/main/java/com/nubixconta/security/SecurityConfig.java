package com.nubixconta.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        // Protegemos todos los métodos de estas rutas:
                        .requestMatchers("/api/v1/users/**").authenticated()
                        .requestMatchers("/api/v1/accounts-receivable/**").authenticated()
                        .requestMatchers("/api/v1/collection-detail/**").authenticated()
                        .requestMatchers("/api/v1/collection-entry/**").authenticated()
                        .requestMatchers("/api/v1/sales/**").authenticated()
                        .requestMatchers("/api/v1/customers/**").authenticated()
                        .requestMatchers("/api/v1/credit-notes/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}