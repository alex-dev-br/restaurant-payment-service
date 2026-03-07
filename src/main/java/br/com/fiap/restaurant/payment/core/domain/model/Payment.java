package br.com.fiap.restaurant.payment.core.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class Payment {
    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Payment(
            UUID id,
            UUID orderId,
            BigDecimal amount,
            PaymentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        validate(orderId, amount, status, createdAt, updatedAt);

        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment createPending(UUID orderId, BigDecimal amount) {
        OffsetDateTime now = OffsetDateTime.now();

        return new Payment(
                UUID.randomUUID(),
                orderId,
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

    private void validate(
            UUID orderId,
            BigDecimal amount,
            PaymentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId é obrigatório");
        }

        if (amount == null) {
            throw new IllegalArgumentException("amount é obrigatório");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount deve ser maior que zero");
        }

        if (status == null) {
            throw new IllegalArgumentException("status é obrigatório");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt é obrigatório");
        }

        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt é obrigatório");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
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


    public void markAsPending() {
        this.status = PaymentStatus.PENDING;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isApproved() {
        return PaymentStatus.APPROVED.equals(this.status);
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

