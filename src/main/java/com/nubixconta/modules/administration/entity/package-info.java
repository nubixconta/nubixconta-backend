/**
 * Este archivo centraliza las definiciones de filtros de Hibernate para toda la aplicación.
 * La anotación @FilterDef solo debe declararse UNA VEZ por nombre de filtro.
 * Colocarla aquí, junto a las entidades de administración fundamentales como Company y User,
 * establece una ubicación lógica y única para la configuración de dominio global.
 */
@org.hibernate.annotations.FilterDefs({
        @org.hibernate.annotations.FilterDef(
                name = "tenantFilter",
                parameters = { @org.hibernate.annotations.ParamDef(name = "companyId", type = Integer.class) }
        )
})
package com.nubixconta.modules.administration.entity;