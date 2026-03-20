package br.com.fiap.restaurant.payment.core.gateway;

import br.com.fiap.restaurant.payment.core.domain.model.PaymentOutboxEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepositoryGateway {

    PaymentOutboxEvent save(PaymentOutboxEvent event);

    List<PaymentOutboxEvent> findPending(int limit);

    void markPublished(UUID eventId, OffsetDateTime publishedAt);
}
