package br.com.fiap.restaurant.payment.core.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class Payment {

    private UUID id;
    private Long orderId;
    private UUID clientId;
    private BigDecimal amount;
    private PaymentStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Payment(
            UUID id,
            Long orderId,
            UUID clientId,
            BigDecimal amount,
            PaymentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        validate(id, orderId, clientId, amount, status, createdAt, updatedAt);

        this.id = id;
        this.orderId = orderId;
        this.clientId = clientId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment createPending(Long orderId, UUID clientId, BigDecimal amount) {
        OffsetDateTime now = OffsetDateTime.now();

        return new Payment(
                UUID.randomUUID(),
                orderId,
                clientId,
                amount,
                PaymentStatus.PENDING,
                now,
                now
        );
    }

    public void approve() {
        this.status = PaymentStatus.APPROVED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markAsPending() {
        this.status = PaymentStatus.PENDING;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isApproved() {
        return PaymentStatus.APPROVED.equals(this.status);
    }

    private static void validate(
            UUID id,
            Long orderId,
            UUID clientId,
            BigDecimal amount,
            PaymentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException("O identificador do pagamento (id) é obrigatório.");
        }

        if (orderId == null) {
            throw new IllegalArgumentException("O identificador do pedido (orderId) é obrigatório.");
        }

        if (clientId == null) {
            throw new IllegalArgumentException("O identificador do cliente (clientId) é obrigatório.");
        }

        if (amount == null) {
            throw new IllegalArgumentException("O valor do pagamento (amount) é obrigatório.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero.");
        }

        if (status == null) {
            throw new IllegalArgumentException("O status do pagamento é obrigatório.");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("A data de criação do pagamento (createdAt) é obrigatória.");
        }

        if (updatedAt == null) {
            throw new IllegalArgumentException("A data de atualização do pagamento (updatedAt) é obrigatória.");
        }
    }

    public UUID getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment payment)) return false;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
