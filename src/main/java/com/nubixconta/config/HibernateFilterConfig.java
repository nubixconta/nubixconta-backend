package com.nubixconta.config; // O com.nubixconta.security si lo prefieres

import com.nubixconta.security.TenantContext;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Configuración de Aspectos de Spring para activar dinámicamente los filtros de Hibernate.
 * Esta clase es el "pegamento" entre nuestro TenantContext y el ORM (Hibernate).
 * Intercepta las llamadas a los repositorios y, si hay una empresa seleccionada en el contexto,
 * activa el filtro 'tenantFilter' para esa sesión de base de datos.
 */
@Aspect
@Component
public class HibernateFilterConfig {

    private final EntityManager entityManager;

    // Inyectamos el EntityManager, que es la puerta de entrada a la sesión de Hibernate.
    public HibernateFilterConfig(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Este método se ejecuta ANTES de cualquier método en cualquier clase que termine en "Repository"
     * dentro de los módulos de la aplicación.
     * Su única responsabilidad es activar el filtro de Hibernate.
     */
    @Before("execution(* com.nubixconta.modules..*Repository.*(..))")
    public void activateTenantFilter() {
        // Obtenemos la sesión de Hibernate del EntityManager. La sesión es la que gestiona los filtros.
        final Session session = entityManager.unwrap(Session.class);

        // Consultamos nuestro TenantContext.
        // El método .ifPresent() es una forma elegante de ejecutar código solo si el Optional contiene un valor.
        TenantContext.getCurrentTenant().ifPresent(tenantId -> {
            // Si hay un tenantId en el contexto, activamos el filtro llamado "tenantFilter"
            // y le pasamos el ID de la empresa como parámetro.
            session.enableFilter("tenantFilter").setParameter("companyId", tenantId);
        });
    }
}