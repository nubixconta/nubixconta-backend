package com.nubixconta.modules.inventory.service;

import com.nubixconta.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.nubixconta.modules.inventory.dto.product.*;
import com.nubixconta.modules.inventory.entity.Product;
import com.nubixconta.modules.inventory.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    public List<ProductResponseDTO> findAll() {
        return productRepository.findAll().stream()
                .map(p -> modelMapper.map(p, ProductResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> findActive() {
        return productRepository.findByProductStatusTrue().stream()
                .map(p -> modelMapper.map(p, ProductResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> findInactive() {
        return productRepository.findByProductStatusFalse().stream()
                .map(p -> modelMapper.map(p, ProductResponseDTO.class))
                .collect(Collectors.toList());
    }

    public ProductResponseDTO findById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));
        return modelMapper.map(product, ProductResponseDTO.class);
    }

    public ProductResponseDTO findByProductCode(String code) {
        Product product = productRepository.findByProductCode(code)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con código: " + code));
        return modelMapper.map(product, ProductResponseDTO.class);
    }

    public List<ProductResponseDTO> searchActive(Integer id, String code, String name) {
        List<Product> result = productRepository.searchActive(
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
        Product product = modelMapper.map(dto, Product.class);
        product.setIdProduct(null);
        product.setProductStatus(true); // por defecto activo
        Product saved = productRepository.save(product);
        return modelMapper.map(saved, ProductResponseDTO.class);
    }
    @Transactional
    public ProductResponseDTO update(Integer id, ProductUpdateDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));
        modelMapper.map(dto, product); // Solo sobrescribe campos no nulos
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