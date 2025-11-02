package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderServiceImpl orderService;

    private Order order;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        this.orderService = new OrderServiceImpl(this.orderRepository);

        Cart cart = Cart.builder()
                .cartId(4)
                .userId(9)
                .build();

        this.order = Order.builder()
                .orderId(15)
                .orderDate(LocalDateTime.of(2024, 2, 12, 14, 30))
                .orderDesc("test-order")
                .orderFee(99.95)
                .cart(cart)
                .build();

        this.orderDto = OrderDto.builder()
                .orderId(this.order.getOrderId())
                .orderDate(this.order.getOrderDate())
                .orderDesc(this.order.getOrderDesc())
                .orderFee(this.order.getOrderFee())
                .cartDto(CartDto.builder()
                        .cartId(cart.getCartId())
                        .userId(cart.getUserId())
                        .build())
                .build();
    }

    @Test
    void findAllReturnsMappedOrders() {
        when(this.orderRepository.findAll()).thenReturn(List.of(this.order));

        List<OrderDto> result = this.orderService.findAll();

        assertEquals(1, result.size());
        OrderDto dto = result.get(0);
        assertEquals(this.order.getOrderId(), dto.getOrderId());
        assertEquals(this.order.getCart().getCartId(), dto.getCartDto().getCartId());
    }

    @Test
    void findByIdReturnsOrderWhenPresent() {
        when(this.orderRepository.findById(this.order.getOrderId())).thenReturn(Optional.of(this.order));

        OrderDto result = this.orderService.findById(this.order.getOrderId());

        assertEquals(this.order.getOrderId(), result.getOrderId());
        assertEquals(this.order.getOrderFee(), result.getOrderFee());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(this.orderRepository.findById(this.order.getOrderId())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> this.orderService.findById(this.order.getOrderId()));
    }

    @Test
    void savePersistsMappedOrder() {
        when(this.orderRepository.save(any(Order.class))).thenReturn(this.order);

        OrderDto result = this.orderService.save(this.orderDto);

        assertEquals(this.order.getOrderId(), result.getOrderId());
        assertEquals(this.order.getOrderDesc(), result.getOrderDesc());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(this.orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals(this.orderDto.getOrderId(), saved.getOrderId());
        assertEquals(this.orderDto.getCartDto().getCartId(), saved.getCart().getCartId());
    }

    @Test
    void updatePersistsMappedOrder() {
        Order updated = Order.builder()
                .orderId(this.order.getOrderId())
                .orderDate(this.order.getOrderDate())
                .orderDesc(this.order.getOrderDesc())
                .orderFee(120.0)
                .cart(this.order.getCart())
                .build();

        when(this.orderRepository.save(any(Order.class))).thenReturn(updated);

        OrderDto updatedDto = OrderDto.builder()
                .orderId(this.orderDto.getOrderId())
                .orderDate(this.orderDto.getOrderDate())
                .orderDesc(this.orderDto.getOrderDesc())
                .orderFee(120.0)
                .cartDto(this.orderDto.getCartDto())
                .build();

        OrderDto result = this.orderService.update(updatedDto);

        assertEquals(updated.getOrderFee(), result.getOrderFee());
    }

    @Test
    void deleteByIdRemovesMappedEntity() {
        when(this.orderRepository.findById(this.order.getOrderId())).thenReturn(Optional.of(this.order));
        doNothing().when(this.orderRepository).delete(any(Order.class));

        this.orderService.deleteById(this.order.getOrderId());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(this.orderRepository).delete(orderCaptor.capture());
        Order deleted = orderCaptor.getValue();
        assertEquals(this.order.getOrderId(), deleted.getOrderId());
        assertEquals(this.order.getCart().getCartId(), deleted.getCart().getCartId());
    }
}
