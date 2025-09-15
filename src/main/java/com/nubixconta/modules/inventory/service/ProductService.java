package com.nubixconta.modules.inventory.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.modules.administration.service.ChangeHistoryService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.nubixconta.modules.inventory.dto.product.*;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.security.TenantContext;
import com.nubixconta.modules.inventory.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final CompanyRepository companyRepository;
    private final ChangeHistoryService changeHistoryService;

    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

    public List<ProductResponseDTO> findAll() {
        Integer companyId = getCompanyIdFromContext();
        return productRepository.findByCompany_IdOrderByProductNameAsc(companyId).stream()
                .map(p -> modelMapper.map(p, ProductResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> findActive() {
        Integer companyId = getCompanyIdFromContext();
        return productRepository.findByCompany_IdAndProductStatusTrueOrderByProductNameAsc(companyId).stream()
                .map(p -> modelMapper.map(p, ProductResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> findInactive() {
        Integer companyId = getCompanyIdFromContext();
        return productRepository.findByCompany_IdAndProductStatusFalseOrderByProductNameAsc(companyId).stream()
                .map(p -> modelMapper.map(p, ProductResponseDTO.class))
                .collect(Collectors.toList());
    }

    public ProductResponseDTO findById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));
        return modelMapper.map(product, ProductResponseDTO.class);
    }

    public ProductResponseDTO findByProductCode(String code) {
        Integer companyId = getCompanyIdFromContext();
        Product product = productRepository.findByCompany_IdAndProductCode(companyId, code)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con código: " + code));
        return modelMapper.map(product, ProductResponseDTO.class);
    }

    public List<ProductResponseDTO> searchActive( String code, String name) {
        Integer companyId = getCompanyIdFromContext();
        String effectiveCode = (code != null && !code.isBlank()) ? code : null;
        String effectiveName = (name != null && !name.isBlank()) ? name : null;
        // Si vamos a buscar por nombre, añadimos los wildcards '%' aquí, en Java.
        if (effectiveName != null) {
            effectiveName = "%" + effectiveName + "%";
        }
        List<Product> result = productRepository.searchActive(
                companyId,
                effectiveCode,
                effectiveName
        );
        return result.stream()
                .map(p -> modelMapper.map(p, ProductResponseDTO.class))
                .collect(Collectors.toList());
    }
    @Transactional
    public ProductResponseDTO create(ProductCreateDTO dto) {
        Integer companyId = getCompanyIdFromContext();

        // Validación de unicidad DENTRO de la empresa
        if(productRepository.existsByCompany_IdAndProductCode(companyId, dto.getProductCode())) {
            throw new BusinessRuleException("El código de producto '" + dto.getProductCode() + "' ya existe para esta empresa.");
        }

        Product product = modelMapper.map(dto, Product.class);
        product.setIdProduct(null);
        product.setProductStatus(true); // por defecto activo

        // Asignar la empresa del contexto al nuevo producto
        Company companyRef = companyRepository.getReferenceById(companyId);
        product.setCompany(companyRef);

        Product saved = productRepository.save(product);
        // --- REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Creó el producto '%s' (Código: %s) con un stock inicial de %d.",
                saved.getProductName(), saved.getProductCode(), saved.getStockQuantity());
        changeHistoryService.logChange("Inventario - Productos", logMessage);
        // --- FIN REGISTRO ---

        return modelMapper.map(saved, ProductResponseDTO.class);
    }
    @Transactional
    public ProductResponseDTO update(Integer id, ProductUpdateDTO dto) {
        // findById ya está protegido por el filtro de Hibernate
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));

        // Validación de unicidad al actualizar DENTRO de la empresa
        if (dto.getProductCode() != null && !dto.getProductCode().equals(product.getProductCode())) {
            if(productRepository.existsByCompany_IdAndProductCodeAndIdProductNot(product.getCompany().getId(), dto.getProductCode(), id)) {
                throw new BusinessRuleException("El código de producto '" + dto.getProductCode() + "' ya existe para esta empresa.");
            }
        }

        modelMapper.map(dto, product);
        Product updated = productRepository.save(product);
        // --- REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Actualizó los datos del producto '%s' (Código: %s).",
                updated.getProductName(), updated.getProductCode());
        changeHistoryService.logChange("Inventario - Productos", logMessage);
        // --- FIN REGISTRO ---
        return modelMapper.map(updated, ProductResponseDTO.class);
    }


    // --- NUEVO MÉTODO PARA DESACTIVAR ---
    @Transactional
    public void deactivate(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));

        if (!product.getProductStatus()) {
            throw new BusinessRuleException("El producto '" + product.getProductName() + "' ya se encuentra inactivo.");
        }

        product.setProductStatus(false);
        productRepository.save(product);

        // --- REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Desactivó el producto '%s' (Código: %s).",
                product.getProductName(), product.getProductCode());
        changeHistoryService.logChange("Inventario - Productos", logMessage);
        // --- FIN REGISTRO ---
    }

    // --- NUEVO MÉTODO PARA ACTIVAR ---
    @Transactional
    public void activate(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));

        if (product.getProductStatus()) {
            throw new BusinessRuleException("El producto '" + product.getProductName() + "' ya se encuentra activo.");
        }

        product.setProductStatus(true);
        productRepository.save(product);

        // --- REGISTRO EN BITÁCORA ---
        String logMessage = String.format("Reactivó el producto '%s' (Código: %s).",
                product.getProductName(), product.getProductCode());
        changeHistoryService.logChange("Inventario - Productos", logMessage);
        // --- FIN REGISTRO ---
    }

    // Solo para uso interno de servicios que necesiten la entidad real (no para exponerla al frontend)
    public Product findEntityById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto con ID " + id + " no encontrado"));
    }

}