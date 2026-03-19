package br.com.fiap.restaurant.payment.infra.messaging.outbound.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentEventMessage(
        UUID paymentId,
        Long orderId,
        UUID clientId,
        BigDecimal amount,
        String status,
        OffsetDateTime occurredAt
) {
}
