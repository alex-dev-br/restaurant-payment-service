package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.infra.messaging.config.KafkaTopicsProperties;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.producer.PaymentEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaPaymentEventPublisherAdapterTest {

    private PaymentEventProducer paymentEventProducer;
    private KafkaTopicsProperties kafkaTopicsProperties;
    private KafkaPaymentEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        paymentEventProducer = mock(PaymentEventProducer.class);

        kafkaTopicsProperties = new KafkaTopicsProperties();
        kafkaTopicsProperties.setPaymentApproved("pagamento.aprovado");
        kafkaTopicsProperties.setPaymentPending("pagamento.pendente");

        adapter = new KafkaPaymentEventPublisherAdapter(paymentEventProducer, kafkaTopicsProperties);
    }

    @Test
    void shouldPublishApprovedEvent() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Payment payment = Payment.createPending(orderId, clientId, new BigDecimal("100.00"));
        payment.approve();

        adapter.publishApproved(payment);

        ArgumentCaptor<PaymentEventMessage> messageCaptor =
                ArgumentCaptor.forClass(PaymentEventMessage.class);

        verify(paymentEventProducer, times(1)).send(
                eq("pagamento.aprovado"),
                eq(orderId.toString()),
                messageCaptor.capture()
        );

        PaymentEventMessage message = messageCaptor.getValue();

        assertNotNull(message);
        assertEquals(payment.getId(), message.paymentId());
        assertEquals(orderId, message.orderId());
        assertEquals(clientId, message.clientId());
        assertEquals(new BigDecimal("100.00"), message.amount());
        assertEquals("APPROVED", message.status());
        assertNotNull(message.occurredAt());
    }

    @Test
    void shouldPublishPendingEvent() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Payment payment = Payment.createPending(orderId, clientId, new BigDecimal("55.90"));

        adapter.publishPending(payment);

        ArgumentCaptor<PaymentEventMessage> messageCaptor =
                ArgumentCaptor.forClass(PaymentEventMessage.class);

        verify(paymentEventProducer, times(1)).send(
                eq("pagamento.pendente"),
                eq(orderId.toString()),
                messageCaptor.capture()
        );

        PaymentEventMessage message = messageCaptor.getValue();

        assertNotNull(message);
        assertEquals(payment.getId(), message.paymentId());
        assertEquals(orderId, message.orderId());
        assertEquals(clientId, message.clientId());
        assertEquals(new BigDecimal("55.90"), message.amount());
        assertEquals("PENDING", message.status());
        assertNotNull(message.occurredAt());
    }
}
