package br.com.fiap.restaurant.payment.core.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void shouldCreatePendingPaymentSuccessfully() {
        Long orderId = 1L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("89.90");

        Payment payment = Payment.createPending(orderId, clientId, amount);

        assertNotNull(payment.getId());
        assertEquals(orderId, payment.getOrderId());
        assertEquals(clientId, payment.getClientId());
        assertEquals(new BigDecimal("89.90"), payment.getAmount());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNotNull(payment.getCreatedAt());
        assertNotNull(payment.getUpdatedAt());
    }

    @Test
    void shouldApprovePaymentSuccessfully() {
        Payment payment = new Payment(
                UUID.randomUUID(),
                1L,
                UUID.randomUUID(),
                new BigDecimal("35.50"),
                PaymentStatus.PENDING,
                OffsetDateTime.now().minusMinutes(1),
                OffsetDateTime.now().minusMinutes(1)
        );

        payment.approve();

        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertNotNull(payment.getUpdatedAt());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsZero() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        1L,
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        PaymentStatus.PENDING,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                )
        );

        assertEquals("O valor do pagamento deve ser maior que zero.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        null,
                        UUID.randomUUID(),
                        new BigDecimal("10.00"),
                        PaymentStatus.PENDING,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                )
        );

        assertEquals("O identificador do pedido (orderId) é obrigatório.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenClientIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(
                        UUID.randomUUID(),
                        1L,
                        null,
                        new BigDecimal("10.00"),
                        PaymentStatus.PENDING,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                )
        );

        assertEquals("O identificador do cliente (clientId) é obrigatório.", exception.getMessage());
    }
}