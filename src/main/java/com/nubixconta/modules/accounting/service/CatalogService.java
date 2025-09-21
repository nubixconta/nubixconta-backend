package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.accounting.dto.catalog.CatalogSummaryDTO; // El DTO que ya creamos
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.repository.CatalogRepository;
import com.nubixconta.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CatalogRepository catalogRepository;
    // ModelMapper no es necesario aquí, el mapeo es simple y explícito.

    /**
     * MÉTODO CRÍTICO para PurchaseService.
     * Busca una entrada del catálogo por su ID.
     * Lanza una excepción si no se encuentra o no está activa.
     * La seguridad multi-tenant es manejada por el filtro de Hibernate.
     *
     * @param id El ID de la entrada del catálogo.
     * @return La entidad Catalog completa si es válida.
     */
    public Catalog findEntityById(Integer id) {
        Catalog catalog = catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La cuenta contable con ID de catálogo " + id + " no fue encontrada."));

        if (!catalog.isActivo() || !catalog.getAccount().isPostable()) {
            throw new BusinessRuleException("La cuenta contable '" + catalog.getAccount().getAccountName() + "' no está activa o no permite movimientos.");
        }

        return catalog;
    }

    /**
     * Busca cuentas activas y que aceptan movimientos para la empresa actual, basándose en un término de búsqueda.
     * Este método será consumido por el frontend.
     *
     * @param term El término a buscar (puede ser vacío para traer todo).
     * @return Lista de CatalogSummaryDTO.
     */
    public List<CatalogSummaryDTO> searchActiveByTerm(String term) {
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("No se ha seleccionado una empresa en el contexto."));

        String searchTerm = (term == null) ? "" : term.trim();

        List<Catalog> catalogs = catalogRepository.searchActiveAndPostableByTerm(companyId, searchTerm);

        return catalogs.stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    // Helper privado para mapear la entidad a un DTO seguro para la API.
    private CatalogSummaryDTO mapToSummaryDTO(Catalog catalog) {
        CatalogSummaryDTO dto = new CatalogSummaryDTO();
        dto.setId(catalog.getId()); // ID de la tabla 'catalog'
        dto.setAccountCode(catalog.getAccount().getGeneratedCode());
        dto.setAccountName(catalog.getAccount().getAccountName());
        return dto;
    }
}