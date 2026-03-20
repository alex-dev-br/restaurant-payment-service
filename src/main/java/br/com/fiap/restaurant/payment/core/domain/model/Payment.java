package br.com.fiap.restaurant.payment.core.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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
    private int retryCount;
    private OffsetDateTime lastRetryAt;
    private OffsetDateTime nextRetryAt;

    public Payment(
            UUID id,
            Long orderId,
            UUID clientId,
            BigDecimal amount,
            PaymentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(
                id,
                orderId,
                clientId,
                amount,
                status,
                createdAt,
                updatedAt,
                0,
                null,
                null
        );
    }

    public Payment(
            UUID id,
            Long orderId,
            UUID clientId,
            BigDecimal amount,
            PaymentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            int retryCount,
            OffsetDateTime lastRetryAt,
            OffsetDateTime nextRetryAt
    ) {
        BigDecimal normalizedAmount = normalizeAmount(amount);

        validate(
                id,
                orderId,
                clientId,
                normalizedAmount,
                status,
                createdAt,
                updatedAt,
                retryCount
        );

        this.id = id;
        this.orderId = orderId;
        this.clientId = clientId;
        this.amount = normalizedAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.retryCount = retryCount;
        this.lastRetryAt = lastRetryAt;
        this.nextRetryAt = nextRetryAt;
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
                now,
                0,
                null,
                null
        );
    }

    public void approve() {
        this.status = PaymentStatus.APPROVED;
        this.updatedAt = OffsetDateTime.now();
        clearRetryMetadata();
    }

    public void markAsPending() {
        this.status = PaymentStatus.PENDING;
        this.updatedAt = OffsetDateTime.now();
    }

    public void registerRetryFailure(Duration backoff) {
        OffsetDateTime now = OffsetDateTime.now();

        this.status = PaymentStatus.PENDING;
        this.updatedAt = now;
        this.retryCount = this.retryCount + 1;
        this.lastRetryAt = now;
        this.nextRetryAt = now.plus(backoff);
    }

    private void clearRetryMetadata() {
        this.retryCount = 0;
        this.lastRetryAt = null;
        this.nextRetryAt = null;
    }

    public boolean isApproved() {
        return PaymentStatus.APPROVED.equals(this.status);
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "O valor do pagamento deve ter no máximo 2 casas decimais.",
                    exception
            );
        }
    }

    private static void validate(
            UUID id,
            Long orderId,
            UUID clientId,
            BigDecimal amount,
            PaymentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            int retryCount
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

        if (retryCount < 0) {
            throw new IllegalArgumentException("O contador de tentativas de retry não pode ser negativo.");
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

    public int getRetryCount() {
        return retryCount;
    }

    public OffsetDateTime getLastRetryAt() {
        return lastRetryAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
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
