package br.com.fiap.restaurant.payment.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_messages")
public class ProcessedMessageEntity {

    @Id
    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "message_type", nullable = false, length = 100)
    private String messageType;

    @Column(name = "aggregate_key", nullable = false, length = 255)
    private String aggregateKey;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    protected ProcessedMessageEntity() {
    }

    public ProcessedMessageEntity(
            UUID messageId,
            String messageType,
            String aggregateKey,
            OffsetDateTime processedAt
    ) {
        this.messageId = messageId;
        this.messageType = messageType;
        this.aggregateKey = aggregateKey;
        this.processedAt = processedAt;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getAggregateKey() {
        return aggregateKey;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
