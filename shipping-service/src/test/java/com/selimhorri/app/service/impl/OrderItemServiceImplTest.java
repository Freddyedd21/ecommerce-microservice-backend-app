package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
import com.selimhorri.app.repository.OrderItemRepository;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RestTemplate restTemplate;

    private OrderItemServiceImpl orderItemService;

    private OrderItem orderItem;
    private OrderItemId orderItemId;
    private ProductDto productDto;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        this.orderItemService = new OrderItemServiceImpl(this.orderItemRepository, this.restTemplate);

        this.orderItem = OrderItem.builder()
                .productId(21)
                .orderId(31)
                .orderedQuantity(2)
                .build();
        this.orderItemId = new OrderItemId(this.orderItem.getProductId(), this.orderItem.getOrderId());

        this.productDto = ProductDto.builder()
                .productId(this.orderItem.getProductId())
                .productTitle("Laptop")
                .build();
        this.orderDto = OrderDto.builder()
                .orderId(this.orderItem.getOrderId())
                .orderDesc("shipping-order")
                .build();
    }

    @Test
    void findAllReturnsOrderItemsWithRemoteData() {
        when(this.orderItemRepository.findAll()).thenReturn(List.of(this.orderItem));
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + this.orderItem.getProductId(),
                ProductDto.class))
                .thenReturn(this.productDto);
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + this.orderItem.getOrderId(),
                OrderDto.class))
                .thenReturn(this.orderDto);

        List<OrderItemDto> result = this.orderItemService.findAll();

        assertEquals(1, result.size());
        OrderItemDto dto = result.get(0);
        assertEquals(this.orderItem.getProductId(), dto.getProductId());
        assertEquals(this.orderItem.getOrderId(), dto.getOrderId());
        assertSame(this.productDto, dto.getProductDto());
        assertSame(this.orderDto, dto.getOrderDto());
        verify(this.restTemplate).getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + this.orderItem.getProductId(),
                ProductDto.class);
        verify(this.restTemplate).getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + this.orderItem.getOrderId(),
                OrderDto.class);
    }

    @Test
    void findByIdReturnsOrderItemWhenPresent() {
        when(this.orderItemRepository.findById(null)).thenReturn(Optional.of(this.orderItem));
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + this.orderItem.getProductId(),
                ProductDto.class))
                .thenReturn(this.productDto);
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + this.orderItem.getOrderId(),
                OrderDto.class))
                .thenReturn(this.orderDto);

        OrderItemDto result = this.orderItemService.findById(this.orderItemId);

        assertEquals(this.orderItem.getProductId(), result.getProductId());
        assertEquals(this.orderItem.getOrderId(), result.getOrderId());
        assertSame(this.productDto, result.getProductDto());
        assertSame(this.orderDto, result.getOrderDto());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(this.orderItemRepository.findById(null)).thenReturn(Optional.empty());

        assertThrows(OrderItemNotFoundException.class, () -> this.orderItemService.findById(this.orderItemId));
    }

    @Test
    void savePersistsMappedOrderItem() {
        OrderItemDto input = OrderItemDto.builder()
                .productId(this.orderItem.getProductId())
                .orderId(this.orderItem.getOrderId())
                .orderedQuantity(this.orderItem.getOrderedQuantity())
                .productDto(ProductDto.builder().productId(this.orderItem.getProductId()).build())
                .orderDto(OrderDto.builder().orderId(this.orderItem.getOrderId()).build())
                .build();

        when(this.orderItemRepository.save(any(OrderItem.class))).thenReturn(this.orderItem);

        OrderItemDto result = this.orderItemService.save(input);

        assertEquals(this.orderItem.getProductId(), result.getProductId());
        assertEquals(this.orderItem.getOrderId(), result.getOrderId());

        ArgumentCaptor<OrderItem> orderItemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(this.orderItemRepository).save(orderItemCaptor.capture());
        OrderItem saved = orderItemCaptor.getValue();
        assertEquals(input.getProductId(), saved.getProductId());
        assertEquals(input.getOrderId(), saved.getOrderId());
        assertEquals(input.getOrderedQuantity(), saved.getOrderedQuantity());
    }

    @Test
    void updatePersistsMappedOrderItem() {
        OrderItem updated = OrderItem.builder()
                .productId(this.orderItem.getProductId())
                .orderId(this.orderItem.getOrderId())
                .orderedQuantity(5)
                .build();

        when(this.orderItemRepository.save(any(OrderItem.class))).thenReturn(updated);

        OrderItemDto result = this.orderItemService.update(OrderItemDto.builder()
                .productId(updated.getProductId())
                .orderId(updated.getOrderId())
                .orderedQuantity(updated.getOrderedQuantity())
                .productDto(ProductDto.builder().productId(updated.getProductId()).build())
                .orderDto(OrderDto.builder().orderId(updated.getOrderId()).build())
                .build());

        assertEquals(updated.getOrderedQuantity(), result.getOrderedQuantity());
    }

    @Test
    void deleteByIdDelegatesToRepository() {
        doNothing().when(this.orderItemRepository).deleteById(eq(this.orderItemId));

        this.orderItemService.deleteById(this.orderItemId);

        verify(this.orderItemRepository).deleteById(this.orderItemId);
    }
}
