package com.nubixconta.security;

import java.util.Optional;

/**
 * Clase de utilidad para gestionar el ID de la empresa (inquilino) activa en el contexto de la petición actual.
 * Utiliza ThreadLocal para garantizar que el ID de la empresa esté aislado entre diferentes hilos (peticiones),
 * lo que es fundamental para la seguridad en un entorno multi-tenant.
 */
public final class TenantContext {

    // Declara una variable ThreadLocal que solo puede contener el ID de la empresa (Integer).
    // Cada hilo que acceda a esta variable tendrá su propia copia independiente.
    private static final ThreadLocal<Integer> currentTenant = new ThreadLocal<>();

    /**
     * Establece el ID de la empresa activa para la petición actual.
     * Esto será llamado por nuestro JwtFilter después de validar un token que contenga un company_id.
     * @param tenantId El ID de la empresa a establecer en el contexto.
     */
    public static void setCurrentTenant(Integer tenantId) {
        currentTenant.set(tenantId);
    }

    /**
     * Obtiene el ID de la empresa activa del contexto de la petición actual.
     * Se devuelve como un Optional para manejar de forma segura los casos en los que no se ha establecido ninguna empresa
     * (por ejemplo, en peticiones con un token de login genérico).
     * @return un Optional que contiene el ID de la empresa si está presente.
     */
    public static Optional<Integer> getCurrentTenant() {
        return Optional.ofNullable(currentTenant.get());
    }

    /**
     * Limpia el ID de la empresa del contexto actual.
     * ESTE MÉTODO ES CRÍTICO. Debe ser llamado al final de cada petición (en un bloque finally)
     * para prevenir que el ID de la empresa se filtre a la siguiente petición que reutilice el mismo hilo.
     */
    public static void clear() {
        currentTenant.remove();
    }
}