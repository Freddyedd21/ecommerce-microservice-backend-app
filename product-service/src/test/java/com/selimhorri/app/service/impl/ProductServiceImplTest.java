package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    private ProductServiceImpl productService;

    private Product product;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        this.productService = new ProductServiceImpl(this.productRepository);

        Category category = Category.builder()
                .categoryId(2)
                .categoryTitle("Peripherals")
                .imageUrl("/images/peripherals")
                .build();

        this.product = Product.builder()
                .productId(10)
                .productTitle("Mouse")
                .imageUrl("/images/mouse")
                .sku("SKU-001")
                .priceUnit(25.5)
                .quantity(100)
                .category(category)
                .build();

        this.productDto = ProductDto.builder()
                .productId(this.product.getProductId())
                .productTitle(this.product.getProductTitle())
                .imageUrl(this.product.getImageUrl())
                .sku(this.product.getSku())
                .priceUnit(this.product.getPriceUnit())
                .quantity(this.product.getQuantity())
                .categoryDto(CategoryDto.builder()
                        .categoryId(category.getCategoryId())
                        .categoryTitle(category.getCategoryTitle())
                        .imageUrl(category.getImageUrl())
                        .build())
                .build();
    }

    @Test
    void findAllReturnsMappedProducts() {
        when(this.productRepository.findAll()).thenReturn(List.of(this.product));

        List<ProductDto> result = this.productService.findAll();

        assertEquals(1, result.size());
        ProductDto dto = result.get(0);
        assertEquals(this.product.getProductId(), dto.getProductId());
        assertEquals(this.product.getCategory().getCategoryId(), dto.getCategoryDto().getCategoryId());
    }

    @Test
    void findByIdReturnsProductWhenPresent() {
        when(this.productRepository.findById(this.product.getProductId())).thenReturn(Optional.of(this.product));

        ProductDto result = this.productService.findById(this.product.getProductId());

        assertEquals(this.product.getProductId(), result.getProductId());
        assertEquals(this.product.getSku(), result.getSku());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(this.productRepository.findById(this.product.getProductId())).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> this.productService.findById(this.product.getProductId()));
    }

    @Test
    void savePersistsMappedProduct() {
        when(this.productRepository.save(any(Product.class))).thenReturn(this.product);

        ProductDto result = this.productService.save(this.productDto);

        assertEquals(this.product.getProductId(), result.getProductId());
        assertEquals(this.product.getSku(), result.getSku());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(this.productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertEquals(this.productDto.getProductId(), saved.getProductId());
        assertEquals(this.productDto.getCategoryDto().getCategoryId(), saved.getCategory().getCategoryId());
    }

    @Test
    void updatePersistsMappedProduct() {
        Product updated = Product.builder()
                .productId(this.product.getProductId())
                .productTitle(this.product.getProductTitle())
                .imageUrl(this.product.getImageUrl())
                .sku(this.product.getSku())
                .priceUnit(30.0)
                .quantity(this.product.getQuantity())
                .category(this.product.getCategory())
                .build();

        when(this.productRepository.save(any(Product.class))).thenReturn(updated);

        ProductDto updatedDto = ProductDto.builder()
                .productId(this.productDto.getProductId())
                .productTitle(this.productDto.getProductTitle())
                .imageUrl(this.productDto.getImageUrl())
                .sku(this.productDto.getSku())
                .priceUnit(30.0)
                .quantity(this.productDto.getQuantity())
                .categoryDto(this.productDto.getCategoryDto())
                .build();

        ProductDto result = this.productService.update(updatedDto);

        assertEquals(updated.getPriceUnit(), result.getPriceUnit());
    }

    @Test
    void deleteByIdRemovesMappedEntity() {
        when(this.productRepository.findById(this.product.getProductId())).thenReturn(Optional.of(this.product));
        doNothing().when(this.productRepository).delete(any(Product.class));

        this.productService.deleteById(this.product.getProductId());

        verify(this.productRepository, times(1)).findById(this.product.getProductId());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(this.productRepository).delete(productCaptor.capture());
        Product deleted = productCaptor.getValue();
        assertEquals(this.product.getProductId(), deleted.getProductId());
        assertEquals(this.product.getSku(), deleted.getSku());
    }
}
