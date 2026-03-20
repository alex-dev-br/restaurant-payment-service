package br.com.fiap.restaurant.payment.core.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class PaymentOutboxEvent {

    private final UUID id;
    private final UUID aggregateId;
    private final PaymentEventType eventType;
    private final String exchangeName;
    private final String routingKey;
    private final String payload;
    private final OutboxStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime publishedAt;

    public PaymentOutboxEvent(
            UUID id,
            UUID aggregateId,
            PaymentEventType eventType,
            String exchangeName,
            String routingKey,
            String payload,
            OutboxStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime publishedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.aggregateId = Objects.requireNonNull(aggregateId);
        this.eventType = Objects.requireNonNull(eventType);
        this.exchangeName = Objects.requireNonNull(exchangeName);
        this.routingKey = Objects.requireNonNull(routingKey);
        this.payload = Objects.requireNonNull(payload);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.publishedAt = publishedAt;
    }

    public PaymentOutboxEvent markPublished(OffsetDateTime publishedAt) {
        return new PaymentOutboxEvent(
                this.id,
                this.aggregateId,
                this.eventType,
                this.exchangeName,
                this.routingKey,
                this.payload,
                OutboxStatus.PUBLISHED,
                this.createdAt,
                publishedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public PaymentEventType getEventType() {
        return eventType;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}