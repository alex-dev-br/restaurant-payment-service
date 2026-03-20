package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.OutboxStatus;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentEventType;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentOutboxEvent;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventTransportGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentOutboxRepositoryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublishPendingPaymentOutboxUseCaseTest {

    private PaymentOutboxRepositoryGateway paymentOutboxRepositoryGateway;
    private PaymentEventTransportGateway paymentEventTransportGateway;
    private PublishPendingPaymentOutboxUseCase publishPendingPaymentOutboxUseCase;

    @BeforeEach
    void setUp() {
        paymentOutboxRepositoryGateway = mock(PaymentOutboxRepositoryGateway.class);
        paymentEventTransportGateway = mock(PaymentEventTransportGateway.class);

        publishPendingPaymentOutboxUseCase = new PublishPendingPaymentOutboxUseCase(
                paymentOutboxRepositoryGateway,
                paymentEventTransportGateway,
                100
        );
    }

    @Test
    void shouldPublishAndMarkAllPendingEventsAsPublished() {
        PaymentOutboxEvent first = buildPendingEvent("payment.approved");
        PaymentOutboxEvent second = buildPendingEvent("payment.pending");

        when(paymentOutboxRepositoryGateway.findPending(100))
                .thenReturn(List.of(first, second));

        publishPendingPaymentOutboxUseCase.execute();

        verify(paymentOutboxRepositoryGateway).findPending(100);
        verify(paymentEventTransportGateway).publish(first);
        verify(paymentEventTransportGateway).publish(second);
        verify(paymentOutboxRepositoryGateway).markPublished(eq(first.getId()), any(OffsetDateTime.class));
        verify(paymentOutboxRepositoryGateway).markPublished(eq(second.getId()), any(OffsetDateTime.class));
    }

    @Test
    void shouldStopAndNotMarkEventAsPublishedWhenTransportFails() {
        PaymentOutboxEvent first = buildPendingEvent("payment.approved");

        when(paymentOutboxRepositoryGateway.findPending(100))
                .thenReturn(List.of(first));

        doThrow(new RuntimeException("rabbit indisponível"))
                .when(paymentEventTransportGateway)
                .publish(first);

        assertThrows(RuntimeException.class, () -> publishPendingPaymentOutboxUseCase.execute());

        verify(paymentOutboxRepositoryGateway).findPending(100);
        verify(paymentEventTransportGateway).publish(first);
        verify(paymentOutboxRepositoryGateway, never()).markPublished(any(UUID.class), any(OffsetDateTime.class));
    }

    private PaymentOutboxEvent buildPendingEvent(String routingKey) {
        return new PaymentOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentEventType.PAYMENT_PENDING,
                "ex.payment",
                routingKey,
                "{\"status\":\"PENDING\"}",
                OutboxStatus.PENDING,
                OffsetDateTime.now(),
                null
        );
    }
}
