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
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    private PaymentServiceImpl paymentService;

    private Payment payment;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        this.paymentService = new PaymentServiceImpl(this.paymentRepository, this.restTemplate);

        this.payment = Payment.builder()
                .paymentId(42)
                .orderId(1001)
                .isPayed(Boolean.TRUE)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        this.orderDto = OrderDto.builder()
                .orderId(this.payment.getOrderId())
                .orderDesc("sample-order")
                .orderFee(19.99)
                .build();
    }

    @Test
    void findAllReturnsPaymentsWithEnrichedOrder() {
        when(this.paymentRepository.findAll()).thenReturn(List.of(this.payment));
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + this.payment.getOrderId(),
                OrderDto.class))
                .thenReturn(this.orderDto);

        List<PaymentDto> result = this.paymentService.findAll();

        assertEquals(1, result.size());
        PaymentDto dto = result.get(0);
        assertEquals(this.payment.getPaymentId(), dto.getPaymentId());
        assertEquals(this.payment.getIsPayed(), dto.getIsPayed());
        assertEquals(this.payment.getPaymentStatus(), dto.getPaymentStatus());
        assertSame(this.orderDto, dto.getOrderDto());
        verify(this.restTemplate)
                .getForObject(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + this.payment.getOrderId(),
                        OrderDto.class);
    }

    @Test
    void findByIdReturnsPaymentWhenPresent() {
        when(this.paymentRepository.findById(this.payment.getPaymentId())).thenReturn(Optional.of(this.payment));
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + this.payment.getOrderId(),
                OrderDto.class))
                .thenReturn(this.orderDto);

        PaymentDto result = this.paymentService.findById(this.payment.getPaymentId());

        assertEquals(this.payment.getPaymentId(), result.getPaymentId());
        assertEquals(this.payment.getIsPayed(), result.getIsPayed());
        assertSame(this.orderDto, result.getOrderDto());
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(this.paymentRepository.findById(this.payment.getPaymentId())).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> this.paymentService.findById(this.payment.getPaymentId()));
    }

    @Test
    void savePersistsMappedPayment() {
        PaymentDto input = PaymentDto.builder()
                .paymentId(this.payment.getPaymentId())
                .isPayed(this.payment.getIsPayed())
                .paymentStatus(this.payment.getPaymentStatus())
                .orderDto(OrderDto.builder().orderId(this.payment.getOrderId()).build())
                .build();

        when(this.paymentRepository.save(any(Payment.class))).thenReturn(this.payment);

        PaymentDto result = this.paymentService.save(input);

        assertEquals(this.payment.getPaymentId(), result.getPaymentId());
        assertEquals(this.payment.getPaymentStatus(), result.getPaymentStatus());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(this.paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertEquals(input.getPaymentId(), saved.getPaymentId());
        assertEquals(input.getOrderDto().getOrderId(), saved.getOrderId());
        assertEquals(input.getIsPayed(), saved.getIsPayed());
    }

    @Test
    void updatePersistsMappedPayment() {
        PaymentDto input = PaymentDto.builder()
                .paymentId(this.payment.getPaymentId())
                .isPayed(Boolean.FALSE)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(OrderDto.builder().orderId(this.payment.getOrderId()).build())
                .build();

        Payment updated = Payment.builder()
                .paymentId(input.getPaymentId())
                .orderId(input.getOrderDto().getOrderId())
                .isPayed(input.getIsPayed())
                .paymentStatus(input.getPaymentStatus())
                .build();

        when(this.paymentRepository.save(any(Payment.class))).thenReturn(updated);

        PaymentDto result = this.paymentService.update(input);

        assertEquals(updated.getIsPayed(), result.getIsPayed());
        assertEquals(updated.getPaymentStatus(), result.getPaymentStatus());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(this.paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertEquals(input.getPaymentId(), saved.getPaymentId());
        assertEquals(input.getOrderDto().getOrderId(), saved.getOrderId());
        assertEquals(input.getIsPayed(), saved.getIsPayed());
    }

    @Test
    void deleteByIdDelegatesToRepository() {
        doNothing().when(this.paymentRepository).deleteById(eq(this.payment.getPaymentId()));

        this.paymentService.deleteById(this.payment.getPaymentId());

        verify(this.paymentRepository).deleteById(this.payment.getPaymentId());
    }
}
