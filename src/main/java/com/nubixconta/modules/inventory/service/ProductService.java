package com.nubixconta.modules.inventory.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
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

    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new BusinessRuleException("No se ha seleccionado una empresa en el contexto."));
    }

    public List<ProductResponseDTO> findAll() {
        return productRepository.findAll().stream()
                .map(p -> modelMapper.map(p, ProductResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> findActive() {
        Integer companyId = getCompanyIdFromContext();
        return productRepository.findByCompany_IdAndProductStatusTrue(companyId).stream()
                .map(p -> modelMapper.map(p, ProductResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> findInactive() {
        Integer companyId = getCompanyIdFromContext();
        return productRepository.findByCompany_IdAndProductStatusTrue(companyId).stream()
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

    public List<ProductResponseDTO> searchActive(Integer id, String code, String name) {
        Integer companyId = getCompanyIdFromContext();
        List<Product> result = productRepository.searchActive(
                companyId,
                id,
                (code != null && !code.isBlank()) ? code : null,
                (name != null && !name.isBlank()) ? name : null
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
        return modelMapper.map(updated, ProductResponseDTO.class);
    }


    public void delete(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Producto no encontrado con ID: " + id);
        }
        productRepository.deleteById(id);
    }
    // Solo para uso interno de servicios que necesiten la entidad real (no para exponerla al frontend)
    public Product findEntityById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto con ID " + id + " no encontrado"));
    }

}