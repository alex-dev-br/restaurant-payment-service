package br.com.fiap.restaurant.payment.infra.persistence.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.OutboxStatus;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentEventType;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentOutboxEvent;
import br.com.fiap.restaurant.payment.infra.persistence.entity.PaymentOutboxEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutboxPersistenceMapper {

    public PaymentOutboxEntity toEntity(PaymentOutboxEvent event) {
        return new PaymentOutboxEntity(
                event.getId(),
                event.getAggregateId(),
                event.getEventType().name(),
                event.getExchangeName(),
                event.getRoutingKey(),
                event.getPayload(),
                event.getStatus().name(),
                event.getCreatedAt(),
                event.getPublishedAt()
        );
    }

    public PaymentOutboxEvent toDomain(PaymentOutboxEntity entity) {
        return new PaymentOutboxEvent(
                entity.getId(),
                entity.getAggregateId(),
                PaymentEventType.valueOf(entity.getEventType()),
                entity.getExchangeName(),
                entity.getRoutingKey(),
                entity.getPayload(),
                OutboxStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getPublishedAt()
        );
    }
}
